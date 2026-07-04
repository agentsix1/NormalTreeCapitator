package dev.normaltreecapitator.util;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * A log column stump to replant after tree capitator finishes.
 */
public record PendingReplant(
        Location location,
        Material logType,
        boolean plantableGround,
        Material expectedSapling
) {

    public PendingReplant(Location location, Material logType, boolean plantableGround) {
        this(location, logType, plantableGround, TreeReplant.saplingForLog(logType));
    }
}
