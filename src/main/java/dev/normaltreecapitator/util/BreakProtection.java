package dev.normaltreecapitator.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Protection-plugin bridge for tree capitator bulk breaks.
 */
public final class BreakProtection {

    public record BreakApproval(Material type, BlockData blockData) {
    }

    private BreakProtection() {
    }

    public static BreakApproval checkBreak(Player player, Block block) {
        if (player == null || block == null) {
            return null;
        }
        Material type = block.getType();
        if (type.isAir()) {
            return null;
        }
        BlockData blockData = block.getBlockData().clone();
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        event.setDropItems(false);
        event.setExpToDrop(0);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }
        return new BreakApproval(type, blockData);
    }

    public static void notifyPlace(Player player, Block block, Material placeType, Block placedAgainst) {
        if (player == null || block == null || placeType == null || placeType.isAir()) {
            return;
        }
        BlockState replaced = block.getState();
        Block against = placedAgainst != null ? placedAgainst : block;
        ItemStack item = new ItemStack(placeType);
        BlockPlaceEvent event = new BlockPlaceEvent(
                block,
                replaced,
                against,
                item,
                player,
                true,
                EquipmentSlot.HAND
        );
        Bukkit.getPluginManager().callEvent(event);
    }
}
