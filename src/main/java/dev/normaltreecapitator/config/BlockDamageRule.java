package dev.normaltreecapitator.config;

import org.bukkit.Material;
import org.bukkit.permissions.Permissible;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * One named section under {@code block-damages} (e.g. {@code logs}, {@code logsvip}).
 */
public final class BlockDamageRule {

    private final String id;
    private final String permission;
    private final int damage;
    private final Set<Material> blocks;

    public BlockDamageRule(String id, String permission, int damage, Set<Material> blocks) {
        this.id = id;
        this.permission = permission == null || permission.isBlank() ? null : permission;
        this.damage = Math.max(0, damage);
        this.blocks = blocks.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(blocks));
    }

    public String id() {
        return id;
    }

    public String permission() {
        return permission;
    }

    public int damage() {
        return damage;
    }

    public Set<Material> blocks() {
        return blocks;
    }

    public boolean covers(Material material) {
        return material != null && blocks.contains(material);
    }

    public boolean requiresPermission() {
        return permission != null;
    }

    public boolean allows(Permissible permissible) {
        if (permission == null) {
            return true;
        }
        return permissible != null && permissible.hasPermission(permission);
    }
}
