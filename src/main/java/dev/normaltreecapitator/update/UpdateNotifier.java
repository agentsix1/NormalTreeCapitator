package dev.normaltreecapitator.update;

import dev.normaltreecapitator.NormalTreeCapitator;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Polls the GitHub {@code version.txt} feed and notifies ops / {@code normaltreecapitator.admin}:
 * <ul>
 *   <li>on plugin enable</li>
 *   <li>when a permitted player joins</li>
 *   <li>when a 30-minute check finds a new (or changed) remote version</li>
 *   <li>every 3 hours as a reminder while still outdated</li>
 * </ul>
 */
public final class UpdateNotifier implements Listener {

    private static final long THIRTY_MINUTES_TICKS = 20L * 60L * 30L;
    private static final long THREE_HOURS_TICKS = 20L * 60L * 60L * 3L;

    private final NormalTreeCapitator plugin;
    private final GithubVersionFetcher fetcher;
    private final AtomicReference<RemoteVersionInfo> latest = new AtomicReference<>();
    /** Last remote version string we already announced to staff (avoids 30-min spam). */
    private final AtomicReference<String> lastAnnouncedRemoteVersion = new AtomicReference<>();

    public UpdateNotifier(NormalTreeCapitator plugin) {
        this.plugin = plugin;
        this.fetcher = new GithubVersionFetcher(plugin);
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getScheduler().runAsync(() -> {
            refreshLatestVersion();
            plugin.getScheduler().runGlobal(() -> announceIfOutdated(true));
        });
        plugin.getScheduler().runGlobalRepeating(this::onThirtyMinutePoll, THIRTY_MINUTES_TICKS);
        plugin.getScheduler().runGlobalRepeating(this::onThreeHourReminder, THREE_HOURS_TICKS);
    }

    public Optional<RemoteVersionInfo> latest() {
        return Optional.ofNullable(latest.get());
    }

    public String localVersion() {
        return plugin.getDescription().getVersion();
    }

    public boolean isOutdated(RemoteVersionInfo remote) {
        return remote != null && VersionComparer.isRemoteNewer(remote.version(), localVersion());
    }

    public void sendVersionReport(CommandSender sender) {
        String local = localVersion();
        plugin.getScheduler().runAsync(() -> {
            refreshLatestVersion();
            Optional<RemoteVersionInfo> remoteOpt = latest();
            plugin.getScheduler().runGlobal(() -> {
                if (sender instanceof Player player && !player.isOnline()) {
                    return;
                }
                Audience audience = sender;
                audience.sendMessage(Component.text("NormalTreeCapitator ", NamedTextColor.GREEN)
                        .append(Component.text(local, NamedTextColor.WHITE)));
                remoteOpt.ifPresentOrElse(remote -> {
                    if (isOutdated(remote)) {
                        audience.sendMessage(buildUpdateMessage(remote));
                    } else {
                        audience.sendMessage(Component.text(
                                "You are running the latest version.", NamedTextColor.GRAY));
                    }
                }, () -> audience.sendMessage(Component.text(
                        "Could not check for updates right now.", NamedTextColor.GRAY)));
            });
        });
    }

    /**
     * Every 30 minutes: re-fetch GitHub {@code version.txt}. If the feed shows a newer version
     * than local and that remote version (or URL) changed since last announce, notify staff + console.
     */
    private void onThirtyMinutePoll() {
        plugin.getScheduler().runAsync(() -> {
            RemoteVersionInfo previous = latest.get();
            refreshLatestVersion();
            RemoteVersionInfo current = latest.get();
            plugin.getScheduler().runGlobal(() -> {
                if (!isOutdated(current)) {
                    return;
                }
                if (!remoteChanged(previous, current) && alreadyAnnounced(current)) {
                    return;
                }
                announceIfOutdated(true);
            });
        });
    }

    /**
     * Every 3 hours: if still outdated, remind console and online staff (even if already announced).
     */
    private void onThreeHourReminder() {
        plugin.getScheduler().runAsync(() -> {
            refreshLatestVersion();
            plugin.getScheduler().runGlobal(() -> announceIfOutdated(false));
        });
    }

    private void refreshLatestVersion() {
        Optional<RemoteVersionInfo> remote = fetcher.fetchLatestRelease();
        remote.ifPresent(latest::set);
        if (remote.isEmpty()) {
            plugin.getLogger().fine("GitHub version check skipped or unavailable.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!isUpdateNotifyTarget(player)) {
            return;
        }
        plugin.getScheduler().runOnEntityLater(player, () -> {
            if (!player.isOnline()) {
                return;
            }
            plugin.getScheduler().runAsync(() -> {
                refreshLatestVersion();
                plugin.getScheduler().runOnEntity(player, () -> notifyIfOutdated(player));
            });
        }, 40L);
    }

    /**
     * @param markAnnounced if true, record this remote version so 30-min polls won't spam it again
     * @return true if an update was announced
     */
    private boolean announceIfOutdated(boolean markAnnounced) {
        RemoteVersionInfo remote = latest.get();
        if (!isOutdated(remote)) {
            return false;
        }
        logOutdatedToConsole(remote);
        notifyOnlineStaff();
        if (markAnnounced) {
            lastAnnouncedRemoteVersion.set(remote.version());
        }
        return true;
    }

    private void notifyOnlineStaff() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            notifyIfOutdated(player);
        }
    }

    private void logOutdatedToConsole(RemoteVersionInfo remote) {
        plugin.getLogger().warning(
                "A newer NormalTreeCapitator is available: " + remote.version()
                        + " (running " + localVersion() + "). Download: " + remote.downloadUrl()
        );
    }

    private void notifyIfOutdated(Player player) {
        if (!isUpdateNotifyTarget(player)) {
            return;
        }
        RemoteVersionInfo remote = latest.get();
        if (!isOutdated(remote)) {
            return;
        }
        player.sendMessage(buildUpdateMessage(remote));
    }

    private boolean alreadyAnnounced(RemoteVersionInfo current) {
        if (current == null) {
            return true;
        }
        String announced = lastAnnouncedRemoteVersion.get();
        return announced != null && announced.equalsIgnoreCase(current.version());
    }

    private static boolean remoteChanged(RemoteVersionInfo previous, RemoteVersionInfo current) {
        if (current == null) {
            return false;
        }
        if (previous == null) {
            return true;
        }
        return !previous.version().equalsIgnoreCase(current.version())
                || !Objects.equals(previous.downloadUrl(), current.downloadUrl());
    }

    static boolean isUpdateNotifyTarget(Player player) {
        return player.isOp() || player.hasPermission("normaltreecapitator.admin");
    }

    static Component buildUpdateMessage(RemoteVersionInfo remote) {
        String url = remote.downloadUrl();
        String remoteVersion = remote.version();
        return Component.text("There is a newer version of Normal Tree Capitator available (", NamedTextColor.YELLOW)
                .append(Component.text(remoteVersion, NamedTextColor.GOLD))
                .append(Component.text("). ", NamedTextColor.YELLOW))
                .append(link("[Click Here]", url))
                .append(Component.text(" or visit ", NamedTextColor.YELLOW))
                .append(link(url, url))
                .append(Component.text(" to get the latest version.", NamedTextColor.YELLOW));
    }

    private static Component link(String text, String url) {
        return Component.text(text, NamedTextColor.AQUA)
                .decoration(TextDecoration.UNDERLINED, true)
                .clickEvent(ClickEvent.openUrl(url));
    }
}
