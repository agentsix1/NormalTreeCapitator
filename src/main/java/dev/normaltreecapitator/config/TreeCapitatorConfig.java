package dev.normaltreecapitator.config;

import dev.normaltreecapitator.NormalTreeCapitator;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class TreeCapitatorConfig {

    private final NormalTreeCapitator plugin;
    private final File configFile;

    private boolean defaultEnabled = true;
    private int maxChain = 100;
    private int searchRadius = 1;
    private boolean mustSneak = false;
    private boolean needTool = true;
    private boolean damageTool = true;
    private boolean breakTool = false;
    private boolean replant = true;
    private boolean invincibleReplant = false;
    private int asyncStart = 150;
    private int blocksPerTick = 100;

    private List<TreeBlockGroup> groups = List.of();
    private Map<Material, TreeBlockGroup> blockToGroup = Map.of();
    private Set<Material> treeBlocks = EnumSet.noneOf(Material.class);
    private Set<Material> treeTools = EnumSet.noneOf(Material.class);

    public TreeCapitatorConfig(NormalTreeCapitator plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
        try (InputStream defaults = plugin.getResource("config.yml")) {
            if (defaults != null) {
                YamlConfiguration embedded = YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(defaults)
                );
                yaml.setDefaults(embedded);
                yaml.options().copyDefaults(true);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not merge default config.yml", e);
        }

        ConfigurationSection playerDefaults = yaml.getConfigurationSection("defaults");
        if (playerDefaults != null) {
            defaultEnabled = playerDefaults.getBoolean("enabled", true);
        }

        ConfigurationSection settings = yaml.getConfigurationSection("settings");
        if (settings != null) {
            maxChain = settings.getInt("max-chain", maxChain);
            searchRadius = clampRadius(settings.getInt("search-radius", searchRadius));
            mustSneak = settings.getBoolean("must-sneak", mustSneak);
            needTool = settings.getBoolean("need-tool", needTool);
            damageTool = settings.getBoolean("damage-tool", damageTool);
            breakTool = settings.getBoolean("break-tool", breakTool);
            replant = settings.getBoolean("replant", replant);
            invincibleReplant = settings.getBoolean("invincible-replant", invincibleReplant);
            asyncStart = Math.max(1, settings.getInt("async-start", asyncStart));
            blocksPerTick = Math.max(1, settings.getInt("blocks-per-tick", blocksPerTick));
        }

        groups = parseGroups(yaml.getConfigurationSection("groups"));
        rebuildCaches();

        try {
            yaml.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save config.yml", e);
        }
    }

    private List<TreeBlockGroup> parseGroups(ConfigurationSection section) {
        List<TreeBlockGroup> parsed = new ArrayList<>();
        if (section == null) {
            return List.of();
        }
        for (String groupId : section.getKeys(false)) {
            ConfigurationSection group = section.getConfigurationSection(groupId);
            if (group == null) {
                continue;
            }
            Set<Material> blocks = parseBlocks(group.getStringList("blocks"), groupId);
            Set<Material> tools = parseTools(group.getStringList("tools"), groupId);
            int groupMax = group.getInt("max-chain", maxChain);
            int groupRadius = clampRadius(group.getInt("search-radius", searchRadius));
            if (!blocks.isEmpty()) {
                parsed.add(new TreeBlockGroup(groupId, blocks, tools, groupMax, groupRadius));
            }
        }
        return List.copyOf(parsed);
    }

    private void rebuildCaches() {
        Map<Material, TreeBlockGroup> index = new HashMap<>();
        Set<Material> blockSet = EnumSet.noneOf(Material.class);
        Set<Material> toolSet = EnumSet.noneOf(Material.class);

        for (TreeBlockGroup group : groups) {
            for (Material block : group.blocks()) {
                index.putIfAbsent(block, group);
            }
            blockSet.addAll(group.blocks());
            toolSet.addAll(group.tools());
        }

        blockToGroup = Map.copyOf(index);
        treeBlocks = Set.copyOf(blockSet);
        treeTools = Set.copyOf(toolSet);
    }

    private Set<Material> parseBlocks(List<String> names, String groupId) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String name : names) {
            Material material = resolveMaterial(name);
            if (material == null) {
                plugin.getLogger().warning("Unknown " + groupId + " block in config.yml: " + name);
                continue;
            }
            if (!material.isBlock()) {
                plugin.getLogger().warning(groupId + " block is not a block type: " + name);
                continue;
            }
            materials.add(material);
        }
        return materials;
    }

    private Set<Material> parseTools(List<String> names, String groupId) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String name : names) {
            Material material = resolveMaterial(name);
            if (material == null) {
                plugin.getLogger().warning("Unknown " + groupId + " tool in config.yml: " + name);
                continue;
            }
            if (!material.isItem()) {
                plugin.getLogger().warning(groupId + " tool is not an item type: " + name);
                continue;
            }
            materials.add(material);
        }
        return materials;
    }

    static Material resolveMaterial(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = name.trim();
        int colon = key.indexOf(':');
        if (colon >= 0) {
            key = key.substring(colon + 1);
        }
        key = key.toUpperCase(Locale.ROOT);
        return Material.matchMaterial(key);
    }

    private static int clampRadius(int radius) {
        return Math.max(1, Math.min(5, radius));
    }

    public TreeBlockGroup groupFor(Material block) {
        return blockToGroup.get(block);
    }

    public boolean isTreeBlock(Material material) {
        return treeBlocks.contains(material);
    }

    public boolean allowsTreeTool(Material material) {
        return treeTools.isEmpty() || treeTools.contains(material);
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    public int maxChain() {
        return maxChain;
    }

    public int searchRadius() {
        return searchRadius;
    }

    public boolean mustSneak() {
        return mustSneak;
    }

    public boolean needTool() {
        return needTool;
    }

    public boolean damageTool() {
        return damageTool;
    }

    public boolean breakTool() {
        return breakTool;
    }

    public boolean replant() {
        return replant;
    }

    public boolean invincibleReplant() {
        return invincibleReplant;
    }

    public int asyncStart() {
        return asyncStart;
    }

    public int blocksPerTick() {
        return blocksPerTick;
    }
}
