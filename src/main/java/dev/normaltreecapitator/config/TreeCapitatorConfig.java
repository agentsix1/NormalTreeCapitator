package dev.normaltreecapitator.config;

import dev.normaltreecapitator.NormalTreeCapitator;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permissible;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

public final class TreeCapitatorConfig {

    private final NormalTreeCapitator plugin;
    private final File configFile;

    private String language = "EN-us";
    private boolean defaultEnabled = true;
    private int maxChain = 100;
    private int searchRadius = 1;
    private boolean mustSneak = false;
    private boolean needTool = true;
    private boolean damageTool = true;
    private boolean breakTool = false;
    private boolean mergeItemDrops = true;
    private int cooldownTicks = 0;
    private boolean replant = true;
    private boolean invincibleReplant = false;
    private boolean replantConsumeSaplings = true;
    private boolean debug = false;
    private int asyncStart = 150;
    private int blocksPerTick = 100;
    private int asyncDelay = 1;

    private List<TreeBlockGroup> groups = List.of();
    private List<BlockDamageRule> damageRules = List.of();
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
                // groups + block-damages are fully user-owned. Keep them out of
                // defaults so removing / renaming sections is not undone by
                // copyDefaults + save on reload.
                embedded.set("groups", null);
                embedded.set("block-damages", null);
                yaml.setDefaults(embedded);
                yaml.options().copyDefaults(true);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not merge default config.yml", e);
        }

        String configuredLanguage = yaml.getString("language", language);
        if (configuredLanguage != null && !configuredLanguage.isBlank()) {
            language = configuredLanguage.trim();
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
            mergeItemDrops = settings.getBoolean("merge-item-drops", mergeItemDrops);
            cooldownTicks = Math.max(0, settings.getInt("cooldown-ticks", cooldownTicks));
            replant = settings.getBoolean("replant", replant);
            invincibleReplant = settings.getBoolean("invincible-replant", invincibleReplant);
            replantConsumeSaplings = settings.getBoolean("replant-consume-saplings", replantConsumeSaplings);
            debug = settings.getBoolean("debug", debug);
            asyncStart = Math.max(1, settings.getInt("async-start", asyncStart));
            blocksPerTick = Math.max(1, settings.getInt("blocks-per-tick", blocksPerTick));
            asyncDelay = Math.max(0, settings.getInt("async-delay", asyncDelay));
        }

        // Only entries written in the player's config.yml (never jar defaults).
        // Missing section / unlisted blocks fall back to defaultBlockDamage().
        damageRules = parseBlockDamages(yaml.getConfigurationSection("block-damages"));
        groups = parseGroups(yaml.getConfigurationSection("groups"));
        rebuildCaches();

        try {
            yaml.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save config.yml", e);
        }

        plugin.getLogger().info("[TreeCap] config " + configFile.getAbsolutePath()
                + " language=" + language
                + " must-sneak=" + mustSneak
                + " debug=" + debug
                + " async-start=" + asyncStart
                + " replant=" + replant);
    }

    public String language() {
        return language;
    }

    /**
     * Writes {@code language:} to {@code config.yml} and updates the in-memory value.
     *
     * @return {@code true} if the file was saved
     */
    public boolean setLanguage(String languageCode) {
        this.language = languageCode;
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
        yaml.set("language", languageCode);
        try {
            yaml.save(configFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save language to config.yml", e);
            return false;
        }
    }

    private List<BlockDamageRule> parseBlockDamages(ConfigurationSection section) {
        List<BlockDamageRule> rules = new ArrayList<>();
        if (section == null) {
            return List.of();
        }
        for (String ruleId : section.getKeys(false)) {
            if (!section.isSet(ruleId)) {
                continue;
            }
            ConfigurationSection rule = section.getConfigurationSection(ruleId);
            if (rule == null) {
                continue;
            }
            int damage = Math.max(0, rule.getInt("damage", 1));
            String permission = resolveConfigPermission("damage", rule.getString("permission"));
            Set<Material> blocks = EnumSet.noneOf(Material.class);
            for (String blockName : rule.getStringList("blocks")) {
                Material material = resolveMaterial(blockName);
                if (material == null) {
                    plugin.getLogger().warning("Unknown block-damages." + ruleId + " block: " + blockName);
                    continue;
                }
                blocks.add(material);
            }
            if (!blocks.isEmpty()) {
                rules.add(new BlockDamageRule(ruleId, permission, damage, blocks));
            }
        }
        return List.copyOf(rules);
    }

    private List<TreeBlockGroup> parseGroups(ConfigurationSection section) {
        List<TreeBlockGroup> parsed = new ArrayList<>();
        if (section == null) {
            return List.of();
        }
        for (String groupId : section.getKeys(false)) {
            if (!section.isSet(groupId)) {
                continue;
            }
            ConfigurationSection group = section.getConfigurationSection(groupId);
            if (group == null) {
                continue;
            }
            Set<Material> blocks = parseBlocks(group.getStringList("blocks"), groupId);
            Set<Material> tools = parseTools(group.getStringList("tools"), groupId);
            String permission = resolveConfigPermission("group", group.getString("permission"));
            int groupMax = group.getInt("max-chain", maxChain);
            int groupRadius = clampRadius(group.getInt("search-radius", searchRadius));
            if (!blocks.isEmpty()) {
                parsed.add(new TreeBlockGroup(
                        groupId, permission, blocks, tools, groupMax, groupRadius
                ));
            }
        }
        return List.copyOf(parsed);
    }

    /**
     * {@code permission: vip} → {@code normaltreecapitator.damage.vip} / {@code .group.vip}.
     * A value that already contains {@code .} is treated as a full permission node.
     */
    static String resolveConfigPermission(String kind, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.contains(".")) {
            return trimmed;
        }
        return "normaltreecapitator." + kind + "." + trimmed.toLowerCase(Locale.ROOT);
    }

    private void rebuildCaches() {
        Set<Material> blockSet = EnumSet.noneOf(Material.class);
        Set<Material> toolSet = EnumSet.noneOf(Material.class);

        for (TreeBlockGroup group : groups) {
            blockSet.addAll(group.blocks());
            toolSet.addAll(group.tools());
        }

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

    /**
     * Picks the group for a broken block, held tool, and player permissions.
     * <p>
     * Permission-gated groups the player qualifies for are preferred over open groups.
     * With {@code need-tool: true}, the group must also list the held tool.
     */
    public TreeBlockGroup groupFor(Material block, Material tool, Permissible player) {
        if (block == null) {
            return null;
        }
        TreeBlockGroup openMatch = null;
        TreeBlockGroup gatedMatch = null;
        for (TreeBlockGroup group : groups) {
            if (!group.matchesBlock(block)) {
                continue;
            }
            if (needTool && !group.allowsTool(tool)) {
                continue;
            }
            if (group.requiresPermission()) {
                if (group.allows(player)) {
                    gatedMatch = group;
                }
                continue;
            }
            if (openMatch == null) {
                openMatch = group;
            }
        }
        return gatedMatch != null ? gatedMatch : openMatch;
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

    public boolean mergeItemDrops() {
        return mergeItemDrops;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public boolean replant() {
        return replant;
    }

    public boolean invincibleReplant() {
        return invincibleReplant;
    }

    public boolean replantConsumeSaplings() {
        return replantConsumeSaplings;
    }

    public boolean debug() {
        return debug;
    }

    public int asyncStart() {
        return asyncStart;
    }

    public int blocksPerTick() {
        return blocksPerTick;
    }

    public int asyncDelay() {
        return asyncDelay;
    }

    /**
     * Durability cost for a block for this player.
     * Permission-gated damage rules override open rules when the player qualifies.
     */
    public int blockDamage(Material material, Permissible player) {
        if (material == null) {
            return 1;
        }
        Integer gated = null;
        Integer open = null;
        for (BlockDamageRule rule : damageRules) {
            if (!rule.covers(material)) {
                continue;
            }
            if (rule.requiresPermission()) {
                if (rule.allows(player)) {
                    gated = rule.damage();
                }
                continue;
            }
            if (open == null) {
                open = rule.damage();
            }
        }
        if (gated != null) {
            return gated;
        }
        if (open != null) {
            return open;
        }
        return defaultBlockDamage(material);
    }

    /** @deprecated use {@link #blockDamage(Material, Permissible)} */
    public int blockDamage(Material material) {
        return blockDamage(material, null);
    }

    static int defaultBlockDamage(Material material) {
        String name = material.name();
        if (name.endsWith("_LEAVES")
                || name.equals("NETHER_WART_BLOCK")
                || name.equals("WARPED_WART_BLOCK")
                || name.equals("SHROOMLIGHT")) {
            return 0;
        }
        return 1;
    }
}
