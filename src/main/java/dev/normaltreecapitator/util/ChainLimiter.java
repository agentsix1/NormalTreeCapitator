package dev.normaltreecapitator.util;

import dev.normaltreecapitator.config.TreeCapitatorConfig;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
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
        List<BlockPosition> costly = new ArrayList<>(targets.size());
        List<BlockPosition> free = new ArrayList<>(targets.size());

        for (BlockPosition pos : targets) {
            Material type = AdjacentFlooder.safeType(pos.world(), pos.x(), pos.y(), pos.z());
            int cost = type == null ? 1 : config.blockDamage(type);
            if (cost <= 0) {
                // Zero-cost blocks (typically leaves) never spend durability — keep them
                // even after the costly-block budget is exhausted so replant can get saplings.
                free.add(pos);
                continue;
            }
            if (spent + cost > budget) {
                // Stop taking further costly blocks; free foliage is still appended below.
                break;
            }
            spent += cost;
            costly.add(pos);
        }

        if (costly.isEmpty() && free.isEmpty()) {
            return List.of();
        }
        if (free.isEmpty()) {
            return List.copyOf(costly);
        }
        if (costly.isEmpty()) {
            return List.copyOf(free);
        }
        List<BlockPosition> allowed = new ArrayList<>(costly.size() + free.size());
        allowed.addAll(costly);
        allowed.addAll(free);
        return List.copyOf(allowed);
    }
}
