package dev.normaltreecapitator.util;

import dev.normaltreecapitator.config.TreeCapitatorConfig;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Caps a break chain so the tool is not destroyed when {@code break-tool} is false.
 */
public final class ChainLimiter {

    private ChainLimiter() {
    }

    public static List<BlockPosition> limitToToolBudget(
            List<BlockPosition> targets,
            ItemStack tool,
            TreeCapitatorConfig config
    ) {
        if (targets == null || targets.isEmpty()) {
            return targets == null ? List.of() : targets;
        }
        if (config == null || !config.damageTool() || config.breakTool() || ToolHelper.isUnbreakable(tool)) {
            return targets;
        }

        int remaining = ToolHelper.remainingDurabilityPoints(tool);
        if (remaining == Integer.MAX_VALUE) {
            return targets;
        }
        int budget = Math.max(0, remaining - 1);
        int spent = 0;
        int allowed = 0;
        for (BlockPosition pos : targets) {
            Material type = AdjacentFlooder.safeType(pos.world(), pos.x(), pos.y(), pos.z());
            int cost = type == null ? 1 : config.blockDamage(type);
            if (cost <= 0) {
                allowed++;
                continue;
            }
            if (spent + cost > budget) {
                break;
            }
            spent += cost;
            allowed++;
        }
        if (allowed <= 0) {
            return List.of();
        }
        if (allowed >= targets.size()) {
            return targets;
        }
        return List.copyOf(targets.subList(0, allowed));
    }
}
