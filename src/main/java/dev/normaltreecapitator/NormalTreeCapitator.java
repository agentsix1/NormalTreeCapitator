package dev.normaltreecapitator;

import dev.normaltreecapitator.command.TreeCapitatorCommand;
import dev.normaltreecapitator.config.TreeCapitatorConfig;
import dev.normaltreecapitator.listener.TreeCapitatorListener;
import dev.normaltreecapitator.messages.PluginMessages;
import dev.normaltreecapitator.playerdata.PlayerDataStore;
import dev.normaltreecapitator.scheduler.PluginScheduler;
import dev.normaltreecapitator.session.BreakSession;
import dev.normaltreecapitator.session.ChainProgressTracker;
import dev.normaltreecapitator.update.UpdateNotifier;
import dev.normaltreecapitator.util.ServerPlatform;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class NormalTreeCapitator extends JavaPlugin {

    private static NormalTreeCapitator instance;

    private PluginMessages messages;
    private PluginScheduler scheduler;
    private PlayerDataStore playerData;
    private TreeCapitatorConfig config;
    private BreakSession sessions;
    private ChainProgressTracker chainProgress;
    private UpdateNotifier updateNotifier;

    @Override
    public void onEnable() {
        instance = this;

        scheduler = new PluginScheduler(this);
        playerData = new PlayerDataStore(this);
        playerData.enable();
        config = new TreeCapitatorConfig(this);
        config.load();

        messages = new PluginMessages(this);
        messages.load();

        sessions = new BreakSession();
        chainProgress = new ChainProgressTracker();

        getServer().getPluginManager().registerEvents(new TreeCapitatorListener(this), this);
        registerCommand("tc", new TreeCapitatorCommand(this));

        updateNotifier = new UpdateNotifier(this);
        updateNotifier.start();

        int pluginId = 32277;
        new Metrics(this, pluginId);

        getLogger().info("Enabled on " + ServerPlatform.displayName() + ".");
    }

    @Override
    public void onDisable() {
        if (playerData != null) {
            playerData.saveAll();
        }
        instance = null;
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command /" + name + " is missing from plugin.yml");
            return;
        }
        if (executor instanceof org.bukkit.command.TabExecutor tab) {
            command.setExecutor(tab);
            command.setTabCompleter(tab);
        } else if (executor instanceof org.bukkit.command.CommandExecutor exec) {
            command.setExecutor(exec);
        }
    }

    public void reloadAll() {
        config.load();
        messages.load();
    }

    public static NormalTreeCapitator getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NormalTreeCapitator is not enabled");
        }
        return instance;
    }

    public PluginMessages messages() {
        return messages;
    }

    public PluginScheduler getScheduler() {
        return scheduler;
    }

    public PlayerDataStore playerData() {
        return playerData;
    }

    public TreeCapitatorConfig config() {
        return config;
    }

    public BreakSession sessions() {
        return sessions;
    }

    public ChainProgressTracker chainProgress() {
        return chainProgress;
    }

    public UpdateNotifier updateNotifier() {
        return updateNotifier;
    }
}
