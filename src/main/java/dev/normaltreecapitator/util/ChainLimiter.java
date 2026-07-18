package dev.normaltreecapitator.util;

import dev.normaltreecapitator.config.TreeCapitatorConfig;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Caps a break chain so the tool is not destroyed when {@code break-tool} is false.
 * <p>
 * Costly blocks (logs) are taken while durability allows. Connected foliage that
 * costs {@code 0} durability is still included afterward — even when the axe has
 * no remaining budget for more logs — so leaf drops can supply saplings for replant.
 * Foliage that costs durability is never taken past the budget.
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
        Set<String> logFamilies = new HashSet<>();

        // Pass 1 — take costly blocks (logs) while the axe can afford them.
        for (BlockPosition pos : targets) {
            Material type = AdjacentFlooder.safeType(pos.world(), pos.x(), pos.y(), pos.z());
            int cost = type == null ? 1 : config.blockDamage(type);
            if (cost <= 0) {
                continue;
            }
            if (spent + cost > budget) {
                break;
            }
            spent += cost;
            costly.add(pos);
            if (type != null && AdjacentFlooder.isTrunkMaterial(type)) {
                String family = TreeReplant.treeFamily(type);
                if (!family.isEmpty()) {
                    logFamilies.add(family);
                }
            }
        }

        // Pass 2 — append every connected 0-cost block from the full flood-fill list.
        // Trunks are ordered first, so a mid-list budget cut must not skip later leaves.
        // Only foliage matching broken log families is kept when we know those families
        // (so we break leaves that can drop saplings for the stumps we actually cut).
        List<BlockPosition> free = new ArrayList<>(targets.size());
        for (BlockPosition pos : targets) {
            Material type = AdjacentFlooder.safeType(pos.world(), pos.x(), pos.y(), pos.z());
            int cost = type == null ? 1 : config.blockDamage(type);
            if (cost > 0) {
                continue;
            }
            if (type != null && !logFamilies.isEmpty() && isFoliage(type) && !matchesAnyFamily(logFamilies, type)) {
                continue;
            }
            free.add(pos);
        }

        if (costly.isEmpty() && free.isEmpty()) {
            return List.of();
        }
        if (free.isEmpty()) {
            return List.copyOf(costly);
        }
        if (costly.isEmpty()) {
            // No logs in budget — do not strip a canopy with no stumps to replant.
            return List.of();
        }
        List<BlockPosition> allowed = new ArrayList<>(costly.size() + free.size());
        allowed.addAll(costly);
        allowed.addAll(free);
        return List.copyOf(allowed);
    }

    private static boolean isFoliage(Material type) {
        String name = type.name();
        return name.endsWith("_LEAVES")
                || name.equals("NETHER_WART_BLOCK")
                || name.equals("WARPED_WART_BLOCK")
                || name.equals("SHROOMLIGHT")
                || name.contains("MUSHROOM_BLOCK")
                || name.equals("MUSHROOM_STEM");
    }

    private static boolean matchesAnyFamily(Set<String> families, Material candidate) {
        for (String family : families) {
            if (TreeReplant.sameTreeFamily(family, candidate)) {
                return true;
            }
        }
        return false;
    }
}
