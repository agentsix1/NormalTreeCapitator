package dev.normaltreecapitator.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class ToolHelper {

    private ToolHelper() {
    }

    public static boolean isAxe(Material material) {
        return material != null && material.name().endsWith("_AXE");
    }

    public static boolean isUnbreakable(ItemStack tool) {
        if (tool == null) {
            return false;
        }
        ItemMeta meta = tool.getItemMeta();
        return meta != null && meta.isUnbreakable();
    }

    public static int remainingDurabilityPoints(ItemStack tool) {
        if (tool == null || tool.getType().isAir() || isUnbreakable(tool)) {
            return Integer.MAX_VALUE;
        }
        if (!(tool.getItemMeta() instanceof Damageable damageable)) {
            return Integer.MAX_VALUE;
        }
        int max = tool.getType().getMaxDurability();
        if (max <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, max - damageable.getDamage());
    }

    public static boolean axeUsable(ItemStack tool, boolean axeNeeded, boolean breakAxe) {
        if (!axeNeeded) {
            return true;
        }
        if (tool == null || !isAxe(tool.getType())) {
            return false;
        }
        if (breakAxe || isUnbreakable(tool)) {
            return true;
        }
        if (tool.getItemMeta() instanceof Damageable damageable) {
            return damageable.getDamage() < tool.getType().getMaxDurability();
        }
        return true;
    }

    public static boolean damageTool(Player player, ItemStack tool, boolean enabled, boolean breakTool) {
        return damageTool(player, tool, enabled, breakTool, 1);
    }

    public static boolean damageTool(
            Player player,
            ItemStack tool,
            boolean enabled,
            boolean breakTool,
            int amount
    ) {
        if (!enabled || amount <= 0 || tool == null || tool.getType().isAir() || isUnbreakable(tool)) {
            return false;
        }
        if (!(tool.getItemMeta() instanceof Damageable damageable)) {
            return false;
        }
        int max = tool.getType().getMaxDurability();
        if (max <= 0) {
            return false;
        }

        Random random = ThreadLocalRandom.current();
        int unbreaking = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        int damage = damageable.getDamage();
        for (int i = 0; i < amount; i++) {
            if (unbreaking > 0 && random.nextInt(unbreaking + 1) != 0) {
                continue;
            }
            damage++;
            if (damage >= max) {
                break;
            }
        }
        damageable.setDamage(Math.min(damage, max));
        tool.setItemMeta(damageable);

        if (damageable.getDamage() >= max) {
            if (breakTool) {
                tool.setAmount(0);
            } else {
                damageable.setDamage(max - 1);
                tool.setItemMeta(damageable);
            }
            return true;
        }
        return false;
    }
}
