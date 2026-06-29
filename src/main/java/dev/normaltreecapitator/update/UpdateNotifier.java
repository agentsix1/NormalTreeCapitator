package dev.normaltreecapitator.update;

import dev.normaltreecapitator.NormalTreeCapitator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class UpdateNotifier implements Listener {

    private static final long THREE_HOURS_TICKS = 20L * 60L * 60L * 3L;

    private final NormalTreeCapitator plugin;
    private final ModrinthVersionFetcher fetcher;
    private final AtomicReference<String> latestVersion = new AtomicReference<>();

    public UpdateNotifier(NormalTreeCapitator plugin) {
        this.plugin = plugin;
        this.fetcher = new ModrinthVersionFetcher(plugin);
    }

    public void start() {
        refreshLatestVersion();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getScheduler().runGlobalRepeating(this::onPeriodicCheck, THREE_HOURS_TICKS);
    }

    private void onPeriodicCheck() {
        plugin.getScheduler().runAsync(() -> {
            refreshLatestVersion();
            plugin.getScheduler().runGlobal(this::notifyOnlineOperators);
        });
    }

    private void refreshLatestVersion() {
        Optional<String> remote = fetcher.fetchLatestRelease();
        remote.ifPresent(latestVersion::set);
        if (remote.isEmpty()) {
            plugin.getLogger().fine("Modrinth version check skipped or unavailable.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp()) {
            return;
        }
        plugin.getScheduler().runOnEntityLater(player, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (latestVersion.get() == null) {
                plugin.getScheduler().runAsync(() -> {
                    refreshLatestVersion();
                    plugin.getScheduler().runOnEntity(player, () -> notifyIfOutdated(player));
                });
                return;
            }
            notifyIfOutdated(player);
        }, 40L);
    }

    private void notifyOnlineOperators() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                notifyIfOutdated(player);
            }
        }
    }

    private void notifyIfOutdated(Player player) {
        if (!player.isOp()) {
            return;
        }
        String remote = latestVersion.get();
        if (remote == null || remote.isBlank()) {
            return;
        }
        String local = plugin.getDescription().getVersion();
        if (!VersionComparer.isRemoteNewer(remote, local)) {
            return;
        }
        player.sendMessage(buildUpdateMessage());
    }

    static Component buildUpdateMessage() {
        String url = ModrinthVersionFetcher.CHANGELOG_URL;
        return Component.text("There is a newer version of Normal Tree Capitator available. ")
                .append(link("[Click Here]", url))
                .append(Component.text(" or visit "))
                .append(link(url, url))
                .append(Component.text(" to get the latest version"));
    }

    private static Component link(String text, String url) {
        return Component.text(text, NamedTextColor.AQUA)
                .decoration(TextDecoration.UNDERLINED, true)
                .clickEvent(ClickEvent.openUrl(url));
    }
}
