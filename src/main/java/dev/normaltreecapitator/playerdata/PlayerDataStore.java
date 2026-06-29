package dev.normaltreecapitator.playerdata;

import dev.normaltreecapitator.NormalTreeCapitator;
import dev.normaltreecapitator.config.TreeCapitatorConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PlayerDataStore {

    private final NormalTreeCapitator plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataStore(NormalTreeCapitator plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
    }

    public void enable() {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create playerdata folder.");
        }
    }

    public PlayerData get(UUID uuid, TreeCapitatorConfig config) {
        return cache.computeIfAbsent(uuid, id -> loadFromDisk(id, config));
    }

    public void save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) {
            return;
        }
        plugin.getScheduler().runAsync(() -> writeToDisk(uuid, data));
    }

    public void saveAll() {
        for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
            writeToDisk(entry.getKey(), entry.getValue());
        }
    }

    private PlayerData loadFromDisk(UUID uuid, TreeCapitatorConfig config) {
        File file = playerFile(uuid);
        boolean defaultEnabled = config != null && config.defaultEnabled();
        if (!file.exists()) {
            return PlayerData.defaults(defaultEnabled);
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        boolean enabled = yaml.getBoolean("enabled", defaultEnabled);
        return new PlayerData(enabled);
    }

    private void writeToDisk(UUID uuid, PlayerData data) {
        File file = playerFile(uuid);
        FileConfiguration yaml = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
        yaml.set("enabled", data.enabled());
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save player data for " + uuid, e);
        }
    }

    private File playerFile(UUID uuid) {
        return new File(dataFolder, uuid + ".yml");
    }
}
