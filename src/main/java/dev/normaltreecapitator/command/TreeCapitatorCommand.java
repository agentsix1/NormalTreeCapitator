package dev.normaltreecapitator.command;

import dev.normaltreecapitator.NormalTreeCapitator;
import dev.normaltreecapitator.config.TreeCapitatorConfig;
import dev.normaltreecapitator.messages.PluginMessages;
import dev.normaltreecapitator.playerdata.PlayerData;
import dev.normaltreecapitator.playerdata.PlayerDataStore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        if (handleReload(sender, args)) {
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
            suggest(out, args[0], "toggle");
            if (sender.hasPermission("normaltreecapitator.reload")) {
                suggest(out, args[0], "reload");
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            if (sender.hasPermission("normaltreecapitator.toggle.others")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    suggest(out, args[1], player.getName());
                }
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
            if (!sender.hasPermission("normaltreecapitator.toggle.others")) {
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
        if (!sender.hasPermission("normaltreecapitator.reload")) {
            plugin.messages().send(sender, "no-permission-reload");
            return true;
        }
        plugin.reloadAll();
        plugin.messages().send(sender, "reload-success");
        return true;
    }

    private boolean handleHelp(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("help")) {
            return false;
        }
        plugin.messages().send(sender, "help-header", PluginMessages.map("label", label));
        plugin.messages().send(sender, "help-toggle", PluginMessages.map(
                "label", label,
                "feature", featureName("feature-tree-capitator")
        ));
        if (sender.hasPermission("normaltreecapitator.toggle.others")) {
            plugin.messages().send(sender, "help-toggle-player", PluginMessages.map("label", label));
        }
        if (sender.hasPermission("normaltreecapitator.reload")) {
            plugin.messages().send(sender, "help-reload", PluginMessages.map("label", label));
        }
        return true;
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
