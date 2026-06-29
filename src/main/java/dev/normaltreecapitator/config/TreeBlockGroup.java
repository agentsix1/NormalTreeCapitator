package dev.normaltreecapitator.config;

import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class TreeBlockGroup {

    private final String id;
    private final Set<Material> blocks;
    private final Set<Material> tools;
    private final int maxChain;
    private final int searchRadius;

    public TreeBlockGroup(
            String id,
            Set<Material> blocks,
            Set<Material> tools,
            int maxChain,
            int searchRadius
    ) {
        this.id = id;
        this.blocks = Collections.unmodifiableSet(EnumSet.copyOf(blocks));
        this.tools = Collections.unmodifiableSet(EnumSet.copyOf(tools));
        this.maxChain = maxChain;
        this.searchRadius = Math.max(1, Math.min(5, searchRadius));
    }

    public String id() {
        return id;
    }

    public Set<Material> blocks() {
        return blocks;
    }

    public Set<Material> tools() {
        return tools;
    }

    public int maxChain() {
        return maxChain;
    }

    public int searchRadius() {
        return searchRadius;
    }

    public boolean allowsTool(Material material) {
        return tools.isEmpty() || tools.contains(material);
    }

    public boolean isReplantableLog(Material material) {
        if (material.name().contains("STRIPPED_")) {
            return false;
        }
        return material.name().endsWith("_LOG")
                || material == Material.CRIMSON_STEM
                || material == Material.WARPED_STEM;
    }
}
