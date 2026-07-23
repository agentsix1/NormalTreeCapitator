package dev.normaltreecapitator.command;

import dev.normaltreecapitator.NormalTreeCapitator;
import dev.normaltreecapitator.config.TreeCapitatorConfig;
import dev.normaltreecapitator.messages.PluginMessages;
import dev.normaltreecapitator.playerdata.PlayerData;
import dev.normaltreecapitator.playerdata.PlayerDataStore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TreeCapitatorCommand implements TabExecutor {

    private final NormalTreeCapitator plugin;

    public TreeCapitatorCommand(NormalTreeCapitator plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label, "help");
            return true;
        }
        if (handleHelp(sender, label, args)) {
            return true;
        }
        if (handleVersion(sender, args)) {
            return true;
        }
        if (handleReload(sender, args)) {
            return true;
        }
        if (handleStatus(sender, label, args)) {
            return true;
        }
        if (handleToggle(sender, command, args)) {
            return true;
        }
        plugin.messages().send(sender, "unknown-subcommand", PluginMessages.map("label", label));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            suggest(out, args[0], "help");
            suggest(out, args[0], "version");
            suggest(out, args[0], "toggle");
            if (sender.hasPermission("normaltreecapitator.admin.status")) {
                suggest(out, args[0], "status");
            }
            if (sender.hasPermission("normaltreecapitator.admin.reload")) {
                suggest(out, args[0], "reload");
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            if (sender.hasPermission("normaltreecapitator.admin.toggle.others")) {
                suggestPlayerNames(out, args[1], true);
            }
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("status")) {
            if (sender.hasPermission("normaltreecapitator.admin.status")) {
                suggestPlayerNames(out, args[1], false);
            }
        }
        return out;
    }

    private void sendUsage(CommandSender sender, String label, String usage) {
        plugin.messages().send(sender, "usage", PluginMessages.map("label", label, "usage", usage));
    }

    private boolean handleToggle(CommandSender sender, Command command, String[] args) {
        String label = command.getLabel().toLowerCase(Locale.ROOT);
        if (args.length == 0 || !args[0].equalsIgnoreCase("toggle")) {
            return false;
        }

        TreeCapitatorConfig config = plugin.config();
        PlayerDataStore store = plugin.playerData();

        if (args.length >= 2) {
            if (!sender.hasPermission("normaltreecapitator.admin.toggle.others")) {
                plugin.messages().send(sender, "no-permission-toggle-others");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                plugin.messages().send(sender, "player-not-found", PluginMessages.map("player", args[1]));
                return true;
            }
            boolean enabled = toggle(store, target, config);
            String state = stateValue(enabled);
            plugin.messages().send(sender, "toggle-other-sender", PluginMessages.map(
                    "feature", featureName("feature-treecapitator"),
                    "state", state,
                    "target", target.getName()
            ));
            plugin.messages().send(target, "toggle-other-target", PluginMessages.map(
                    "feature", featureName("feature-treecapitator"),
                    "state", state,
                    "sender", sender.getName()
            ));
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "only-players");
            return true;
        }
        if (!sender.hasPermission("normaltreecapitator.toggle")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        boolean enabled = toggle(store, player, config);
        plugin.messages().send(sender, "toggle-self", PluginMessages.map(
                "feature", featureName("feature-treecapitator"),
                "state", stateValue(enabled)
        ));
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            return false;
        }
        if (!sender.hasPermission("normaltreecapitator.admin.reload")) {
            plugin.messages().send(sender, "no-permission-reload");
            return true;
        }
        plugin.reloadAll();
        plugin.messages().send(sender, "reload-success");
        return true;
    }

    private boolean handleStatus(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("status")) {
            return false;
        }
        if (!sender.hasPermission("normaltreecapitator.admin.status")) {
            plugin.messages().send(sender, "no-permission-status");
            return true;
        }

        TreeCapitatorConfig config = plugin.config();
        PlayerDataStore store = plugin.playerData();

        if (args.length >= 2) {
            ResolvedPlayer target = resolvePlayer(args[1]);
            if (target == null) {
                plugin.messages().send(sender, "player-not-found", PluginMessages.map("player", args[1]));
                return true;
            }
            boolean enabled = store.get(target.uuid(), config).enabled();
            plugin.messages().send(sender, "status-other", PluginMessages.map(
                    "feature", featureName("feature-treecapitator"),
                    "state", stateValue(enabled),
                    "target", target.name(),
                    "presence", presenceValue(target.online())
            ));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sendUsage(sender, label, "status <player>");
            return true;
        }
        boolean enabled = store.get(player.getUniqueId(), config).enabled();
        plugin.messages().send(sender, "status-self", PluginMessages.map(
                "feature", featureName("feature-treecapitator"),
                "state", stateValue(enabled)
        ));
        return true;
    }

    private boolean handleVersion(CommandSender sender, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("version")) {
            return false;
        }
        plugin.updateNotifier().sendVersionReport(sender);
        return true;
    }

    private boolean handleHelp(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("help")) {
            return false;
        }
        plugin.messages().send(sender, "help-header", PluginMessages.map("label", label));
        plugin.messages().send(sender, "help-version", PluginMessages.map("label", label));
        plugin.messages().send(sender, "help-toggle", PluginMessages.map(
                "label", label,
                "feature", featureName("feature-tree-capitator")
        ));
        if (sender.hasPermission("normaltreecapitator.admin.toggle.others")) {
            plugin.messages().send(sender, "help-toggle-player", PluginMessages.map("label", label));
        }
        if (sender.hasPermission("normaltreecapitator.admin.status")) {
            plugin.messages().send(sender, "help-status", PluginMessages.map("label", label));
            plugin.messages().send(sender, "help-status-player", PluginMessages.map("label", label));
        }
        if (sender.hasPermission("normaltreecapitator.admin.reload")) {
            plugin.messages().send(sender, "help-reload", PluginMessages.map("label", label));
        }
        return true;
    }

    private ResolvedPlayer resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return new ResolvedPlayer(online.getUniqueId(), online.getName(), true);
        }
        OfflinePlayer match = null;
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            String offlineName = offline.getName();
            if (offlineName != null && offlineName.equalsIgnoreCase(name)) {
                match = offline;
                break;
            }
        }
        if (match == null || match.getUniqueId() == null) {
            return null;
        }
        if (!match.hasPlayedBefore() && !match.isOnline()) {
            return null;
        }
        String resolvedName = match.getName() != null ? match.getName() : name;
        return new ResolvedPlayer(match.getUniqueId(), resolvedName, match.isOnline());
    }

    private void suggestPlayerNames(List<String> out, String typed, boolean onlineOnly) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            suggest(out, typed, player.getName());
        }
        if (onlineOnly) {
            return;
        }
        String prefix = typed.toLowerCase(Locale.ROOT);
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.isOnline()) {
                continue;
            }
            String name = offline.getName();
            if (name == null || !name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                continue;
            }
            if (!out.contains(name)) {
                out.add(name);
            }
            if (out.size() >= 50) {
                break;
            }
        }
    }

    private String presenceValue(boolean online) {
        return plugin.messages().get(online ? "presence-online" : "presence-offline");
    }

    private record ResolvedPlayer(UUID uuid, String name, boolean online) {
    }

    private String featureName(String key) {
        return plugin.messages().get(key);
    }

    private String stateValue(boolean enabled) {
        return plugin.messages().get(enabled ? "state-enabled" : "state-disabled");
    }

    private static boolean toggle(PlayerDataStore store, Player player, TreeCapitatorConfig config) {
        PlayerData data = store.get(player.getUniqueId(), config);
        boolean next = !data.enabled();
        data.setEnabled(next);
        store.save(player.getUniqueId());
        return next;
    }

    private static void suggest(List<String> out, String typed, String candidate) {
        if (candidate.toLowerCase(Locale.ROOT).startsWith(typed.toLowerCase(Locale.ROOT))) {
            out.add(candidate);
        }
    }
}
