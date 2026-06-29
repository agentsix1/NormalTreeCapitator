package dev.normaltreecapitator.util;

import org.bukkit.Location;
import org.bukkit.World;

public record BlockPosition(World world, int x, int y, int z) {

    public static BlockPosition of(Location location) {
        return new BlockPosition(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    public Location toLocation() {
        return new Location(world, x, y, z);
    }
}
