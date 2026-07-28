package dev.normaltreecapitator.command;

import dev.normaltreecapitator.NormalTreeCapitator;
import dev.normaltreecapitator.config.TreeCapitatorConfig;
import dev.normaltreecapitator.messages.PluginMessages;
import dev.normaltreecapitator.playerdata.PlayerData;
import dev.normaltreecapitator.playerdata.PlayerDataStore;
import dev.normaltreecapitator.session.ChainProgressTracker;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TreeCapitatorCommand implements TabExecutor {

    private static final Set<String> SUBCOMMANDS = Set.of(
            "help", "version", "toggle", "status", "reload", "language"
    );

    private final NormalTreeCapitator plugin;

    public TreeCapitatorCommand(NormalTreeCapitator plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleSelfToggleState(sender, label);
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
        if (handleLanguage(sender, label, args)) {
            return true;
        }
        if (handleChainStatus(sender, args)) {
            return true;
        }
        if (handleToggle(sender, command, args)) {
            return true;
        }
        if (handlePlayerLookup(sender, label, args)) {
            return true;
        }
        plugin.messages().send(sender, "unknown-subcommand", PluginMessages.map("label", label));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("normaltreecapitator.help")) {
                suggest(out, args[0], "help");
            }
            if (sender.hasPermission("normaltreecapitator.version")) {
                suggest(out, args[0], "version");
            }
            if (sender.hasPermission("normaltreecapitator.toggle")) {
                suggest(out, args[0], "toggle");
            }
            if (sender.hasPermission("normaltreecapitator.progress")) {
                suggest(out, args[0], "status");
            }
            if (sender.hasPermission("normaltreecapitator.admin.reload")) {
                suggest(out, args[0], "reload");
            }
            if (sender.hasPermission("normaltreecapitator.language")
                    || sender.hasPermission("normaltreecapitator.admin.language")) {
                suggest(out, args[0], "language");
            }
            if (sender.hasPermission("normaltreecapitator.admin.state")) {
                suggestPlayerNames(out, args[0], false);
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("language")) {
            if (sender.hasPermission("normaltreecapitator.admin.language")) {
                suggest(out, args[1], "server");
            }
            if (sender.hasPermission("normaltreecapitator.language")
                    || (!(sender instanceof Player)
                    && sender.hasPermission("normaltreecapitator.admin.language"))) {
                for (String code : plugin.messages().availableLanguages()) {
                    suggest(out, args[1], code);
                }
            }
            return out;
        }
        if (args.length == 3
                && args[0].equalsIgnoreCase("language")
                && args[1].equalsIgnoreCase("server")
                && sender.hasPermission("normaltreecapitator.admin.language")) {
            for (String code : plugin.messages().availableLanguages()) {
                suggest(out, args[2], code);
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            if (sender.hasPermission("normaltreecapitator.admin.toggle.others")) {
                suggestPlayerNames(out, args[1], true);
            }
        }
        return out;
    }

    private boolean handleSelfToggleState(CommandSender sender, String label) {
        if (!(sender instanceof Player player)) {
            sendUsage(sender, label, "help");
            return true;
        }
        if (!sender.hasPermission("normaltreecapitator.status")) {
            plugin.messages().send(sender, "no-permission-status");
            return true;
        }
        boolean enabled = plugin.playerData().get(player.getUniqueId(), plugin.config()).enabled();
        plugin.messages().send(sender, "status-self", PluginMessages.map(
                "feature", featureName(sender, "feature-treecapitator"),
                "state", stateValue(sender, enabled)
        ));
        return true;
    }

    private boolean handlePlayerLookup(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || SUBCOMMANDS.contains(args[0].toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!sender.hasPermission("normaltreecapitator.admin.state")) {
            plugin.messages().send(sender, "no-permission-status-others");
            return true;
        }
        ResolvedPlayer target = resolvePlayer(args[0]);
        if (target == null) {
            plugin.messages().send(sender, "player-not-found", PluginMessages.map("player", args[0]));
            return true;
        }
        boolean enabled = plugin.playerData().get(target.uuid(), plugin.config()).enabled();
        plugin.messages().send(sender, "status-other", PluginMessages.map(
                "feature", featureName(sender, "feature-treecapitator"),
                "state", stateValue(sender, enabled),
                "target", target.name(),
                "presence", presenceValue(sender, target.online())
        ));
        return true;
    }

    private boolean handleChainStatus(CommandSender sender, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("status")) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "only-players");
            return true;
        }
        if (!sender.hasPermission("normaltreecapitator.progress")) {
            plugin.messages().send(sender, "no-permission-progress");
            return true;
        }
        Optional<ChainProgressTracker.ChainJob> job = plugin.chainProgress().get(player.getUniqueId());
        if (job.isEmpty()) {
            plugin.messages().send(sender, "chain-status-idle");
            return true;
        }
        ChainProgressTracker.ChainJob active = job.get();
        plugin.messages().send(sender, "chain-status", PluginMessages.map(
                "done", String.valueOf(active.done()),
                "total", String.valueOf(active.total()),
                "percent", String.valueOf(active.percent()),
                "remaining", String.valueOf(active.remaining()),
                "eta", formatEta(active.estimateRemainingMs()),
                "mode", active.async() ? "async" : "sync"
        ));
        return true;
    }

    private static String formatEta(long remainingMs) {
        if (remainingMs < 0) {
            return "…";
        }
        if (remainingMs < 1000L) {
            return "<1s";
        }
        long totalSeconds = Math.max(1L, (remainingMs + 500L) / 1000L);
        if (totalSeconds < 60L) {
            return "~" + totalSeconds + "s";
        }
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes < 60L) {
            return seconds == 0 ? "~" + minutes + "m" : "~" + minutes + "m " + seconds + "s";
        }
        long hours = minutes / 60L;
        long remMinutes = minutes % 60L;
        return remMinutes == 0 ? "~" + hours + "h" : "~" + hours + "h " + remMinutes + "m";
    }

    private void sendUsage(CommandSender sender, String label, String usage) {
        plugin.messages().send(sender, "usage", PluginMessages.map("label", label, "usage", usage));
    }

    private boolean handleToggle(CommandSender sender, Command command, String[] args) {
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
            String state = stateValue(sender, enabled);
            plugin.messages().send(sender, "toggle-other-sender", PluginMessages.map(
                    "feature", featureName(sender, "feature-treecapitator"),
                    "state", state,
                    "target", target.getName()
            ));
            plugin.messages().send(target, "toggle-other-target", PluginMessages.map(
                    "feature", featureName(target, "feature-treecapitator"),
                    "state", stateValue(target, enabled),
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
                "feature", featureName(sender, "feature-treecapitator"),
                "state", stateValue(sender, enabled)
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

    private boolean handleLanguage(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("language")) {
            return false;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("server")) {
            return handleServerLanguage(sender, label, args.length >= 3 ? args[2] : null);
        }

        if (!(sender instanceof Player player)) {
            if (!sender.hasPermission("normaltreecapitator.admin.language")) {
                plugin.messages().send(sender, "no-permission");
                return true;
            }
            return handleServerLanguage(sender, label, args.length >= 2 ? args[1] : null);
        }

        if (!sender.hasPermission("normaltreecapitator.language")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }

        String available = String.join(", ", plugin.messages().availableLanguages());
        if (args.length < 2) {
            plugin.messages().send(sender, "language-current", PluginMessages.map(
                    "language", plugin.messages().resolveLanguageCode(sender),
                    "languages", available
            ));
            sendUsage(sender, label, "language <code>");
            return true;
        }

        Optional<String> matched = plugin.messages().matchLanguage(args[1]);
        if (matched.isEmpty()) {
            plugin.messages().send(sender, "language-invalid", PluginMessages.map(
                    "language", args[1],
                    "languages", available
            ));
            return true;
        }

        PlayerData data = plugin.playerData().get(player.getUniqueId(), plugin.config());
        data.setLanguage(matched.get());
        plugin.playerData().save(player.getUniqueId());
        plugin.messages().send(sender, "language-set", PluginMessages.map(
                "language", matched.get()
        ));
        return true;
    }

    private boolean handleServerLanguage(CommandSender sender, String label, String codeArg) {
        if (!sender.hasPermission("normaltreecapitator.admin.language")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        String available = String.join(", ", plugin.messages().availableLanguages());
        if (codeArg == null || codeArg.isBlank()) {
            plugin.messages().send(sender, "language-server-current", PluginMessages.map(
                    "language", plugin.messages().activeLanguage(),
                    "languages", available
            ));
            sendUsage(sender, label, "language server <code>");
            return true;
        }
        Optional<String> matched = plugin.messages().matchLanguage(codeArg);
        if (matched.isEmpty()) {
            plugin.messages().send(sender, "language-invalid", PluginMessages.map(
                    "language", codeArg,
                    "languages", available
            ));
            return true;
        }
        if (!plugin.config().setLanguage(matched.get())) {
            plugin.messages().send(sender, "language-save-failed");
            return true;
        }
        plugin.messages().load();
        plugin.messages().send(sender, "language-server-set", PluginMessages.map(
                "language", plugin.messages().activeLanguage()
        ));
        return true;
    }

    private boolean handleVersion(CommandSender sender, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("version")) {
            return false;
        }
        if (!sender.hasPermission("normaltreecapitator.version")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        plugin.updateNotifier().sendVersionReport(sender);
        return true;
    }

    private boolean handleHelp(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("help")) {
            return false;
        }
        if (!sender.hasPermission("normaltreecapitator.help")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        plugin.messages().send(sender, "help-header", PluginMessages.map("label", label));
        if (sender.hasPermission("normaltreecapitator.status")) {
            plugin.messages().send(sender, "help-self", PluginMessages.map("label", label));
        }
        if (sender.hasPermission("normaltreecapitator.admin.state")) {
            plugin.messages().send(sender, "help-player", PluginMessages.map("label", label));
        }
        if (sender.hasPermission("normaltreecapitator.progress")) {
            plugin.messages().send(sender, "help-status", PluginMessages.map("label", label));
        }
        if (sender.hasPermission("normaltreecapitator.version")) {
            plugin.messages().send(sender, "help-version", PluginMessages.map("label", label));
        }
        if (sender.hasPermission("normaltreecapitator.toggle")) {
            plugin.messages().send(sender, "help-toggle", PluginMessages.map(
                    "label", label,
                    "feature", featureName(sender, "feature-tree-capitator")
            ));
        }
        if (sender.hasPermission("normaltreecapitator.admin.toggle.others")) {
            plugin.messages().send(sender, "help-toggle-player", PluginMessages.map("label", label));
        }
        if (sender.hasPermission("normaltreecapitator.language")) {
            plugin.messages().send(sender, "help-language", PluginMessages.map("label", label));
        }
        if (sender.hasPermission("normaltreecapitator.admin.reload")) {
            plugin.messages().send(sender, "help-reload", PluginMessages.map("label", label));
        }
        if (sender.hasPermission("normaltreecapitator.admin.language")) {
            plugin.messages().send(sender, "help-language-server", PluginMessages.map("label", label));
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

    private String presenceValue(CommandSender sender, boolean online) {
        return plugin.messages().get(sender, online ? "presence-online" : "presence-offline");
    }

    private record ResolvedPlayer(UUID uuid, String name, boolean online) {
    }

    private String featureName(CommandSender sender, String key) {
        return plugin.messages().get(sender, key);
    }

    private String stateValue(CommandSender sender, boolean enabled) {
        return plugin.messages().get(sender, enabled ? "state-enabled" : "state-disabled");
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
