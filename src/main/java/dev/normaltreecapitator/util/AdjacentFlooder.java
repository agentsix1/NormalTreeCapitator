package dev.normaltreecapitator.util;

import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class AdjacentFlooder {

    private AdjacentFlooder() {
    }

    public static List<BlockPosition> floodFill(
            BlockPosition origin,
            Predicate<Material> include,
            int maxBlocks,
            int searchRadius
    ) {
        World world = origin.world();
        int radius = Math.max(1, Math.min(5, searchRadius));
        Set<Long> visited = new HashSet<>();
        ArrayDeque<BlockPosition> queue = new ArrayDeque<>();
        List<BlockPosition> result = new ArrayList<>();

        long originKey = pack(origin.x(), origin.y(), origin.z());
        if (!include.test(world.getBlockAt(origin.x(), origin.y(), origin.z()).getType())) {
            return result;
        }
        visited.add(originKey);
        queue.add(origin);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPosition current = queue.poll();
            Material type = world.getBlockAt(current.x(), current.y(), current.z()).getType();
            if (!include.test(type)) {
                continue;
            }
            result.add(current);

            int cx = current.x();
            int cy = current.y();
            int cz = current.z();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        if (result.size() >= maxBlocks) {
                            return result;
                        }
                        BlockPosition next = new BlockPosition(world, cx + dx, cy + dy, cz + dz);
                        long nextKey = pack(next.x(), next.y(), next.z());
                        if (visited.contains(nextKey)) {
                            continue;
                        }
                        Material nextType = world.getBlockAt(next.x(), next.y(), next.z()).getType();
                        if (!include.test(nextType)) {
                            continue;
                        }
                        visited.add(nextKey);
                        queue.add(next);
                    }
                }
            }
        }
        return result;
    }

    private static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38
                | ((long) z & 0x3FFFFFFL) << 12
                | (y & 0xFFFL);
    }
}
