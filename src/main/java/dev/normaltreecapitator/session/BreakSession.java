package dev.normaltreecapitator.session;

import org.bukkit.Location;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents recursive {@link org.bukkit.event.block.BlockBreakEvent} handling while bulk-breaking.
 */
public final class BreakSession {

    private final Set<UUID> cooldown = ConcurrentHashMap.newKeySet();
    private final Set<Location> breaking = ConcurrentHashMap.newKeySet();
    private final Set<UUID> shiftHeld = ConcurrentHashMap.newKeySet();

    public boolean onCooldown(UUID playerId) {
        return cooldown.contains(playerId);
    }

    public void addCooldown(UUID playerId) {
        cooldown.add(playerId);
    }

    public void endCooldown(UUID playerId) {
        cooldown.remove(playerId);
    }

    public boolean markBreaking(Location location) {
        return breaking.add(location);
    }

    public void unmarkBreaking(Location location) {
        breaking.remove(location);
    }

    public boolean isPluginBreak(Location location) {
        return breaking.contains(location);
    }

    public void setShiftHeld(UUID playerId, boolean held) {
        if (held) {
            shiftHeld.add(playerId);
        } else {
            shiftHeld.remove(playerId);
        }
    }

    public boolean isShiftHeld(UUID playerId) {
        return shiftHeld.contains(playerId);
    }
}
