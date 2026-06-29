package dev.normaltreecapitator.util;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class DropHelper {

    private DropHelper() {
    }

    public static Collection<ItemStack> resolveDrops(Block block, ItemStack tool, Player player) {
        return new ArrayList<>(block.getDrops(tool, player));
    }

    public static void dropStacks(org.bukkit.Location dropAt, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }
            int remaining = stack.getAmount();
            int max = stack.getMaxStackSize();
            while (remaining > 0) {
                int chunk = Math.min(remaining, max);
                ItemStack part = stack.clone();
                part.setAmount(chunk);
                dropAt.getWorld().dropItemNaturally(dropAt, part);
                remaining -= chunk;
            }
        }
    }
}
