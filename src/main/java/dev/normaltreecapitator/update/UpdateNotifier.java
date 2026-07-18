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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class UpdateNotifier implements Listener {

    private static final long THREE_HOURS_TICKS = 20L * 60L * 60L * 3L;

    private final NormalTreeCapitator plugin;
    private final PastebinVersionFetcher fetcher;
    private final AtomicReference<RemoteVersionInfo> latest = new AtomicReference<>();

    public UpdateNotifier(NormalTreeCapitator plugin) {
        this.plugin = plugin;
        this.fetcher = new PastebinVersionFetcher(plugin);
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getScheduler().runAsync(() -> {
            refreshLatestVersion();
            plugin.getScheduler().runGlobal(() -> {
                logOutdatedToConsole();
                notifyOnlineStaff();
            });
        });
        plugin.getScheduler().runGlobalRepeating(this::onPeriodicCheck, THREE_HOURS_TICKS);
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

    private void onPeriodicCheck() {
        plugin.getScheduler().runAsync(() -> {
            refreshLatestVersion();
            plugin.getScheduler().runGlobal(this::notifyOnlineStaff);
        });
    }

    private void refreshLatestVersion() {
        Optional<RemoteVersionInfo> remote = fetcher.fetchLatestRelease();
        remote.ifPresent(latest::set);
        if (remote.isEmpty()) {
            plugin.getLogger().fine("Pastebin version check skipped or unavailable.");
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
            if (latest.get() == null) {
                plugin.getScheduler().runAsync(() -> {
                    refreshLatestVersion();
                    plugin.getScheduler().runOnEntity(player, () -> notifyIfOutdated(player));
                });
                return;
            }
            notifyIfOutdated(player);
        }, 40L);
    }

    private void notifyOnlineStaff() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            notifyIfOutdated(player);
        }
    }

    private void logOutdatedToConsole() {
        RemoteVersionInfo remote = latest.get();
        if (!isOutdated(remote)) {
            return;
        }
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
