package dev.normaltreecapitator.session;

import org.bukkit.Location;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents recursive {@link org.bukkit.event.block.BlockBreakEvent} handling while bulk-breaking.
 */
public final class BreakSession {

    private final Set<Location> breaking = ConcurrentHashMap.newKeySet();

    public boolean markBreaking(Location location) {
        return breaking.add(location);
    }

    public void unmarkBreaking(Location location) {
        breaking.remove(location);
    }

    public boolean isPluginBreak(Location location) {
        return breaking.contains(location);
    }
}
