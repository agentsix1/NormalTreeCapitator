package dev.normaltreecapitator.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

    /**
     * @return true if the tool should stop breaking further blocks
     */
    public static boolean damageTool(Player player, ItemStack tool, boolean enabled, boolean breakAxe) {
        if (!enabled || tool == null || tool.getType().isAir() || isUnbreakable(tool)) {
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
        Enchantment unbreakingEnchant = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        int unbreaking = unbreakingEnchant != null ? tool.getEnchantmentLevel(unbreakingEnchant) : 0;
        if (random.nextInt(unbreaking + 1) != 0) {
            return damageable.getDamage() >= max - 1;
        }

        int damage = damageable.getDamage() + 1;
        damageable.setDamage(damage);
        tool.setItemMeta(damageable);

        if (damage >= max) {
            if (breakAxe) {
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
