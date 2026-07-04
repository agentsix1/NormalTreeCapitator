package dev.normaltreecapitator.util;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Flood-fill for tree capitator groups. On Folia, only loaded chunks owned by the
 * current region thread are read (never sync-loaded).
 */
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
        if (world == null || include == null) {
            return List.of();
        }
        int radius = Math.max(1, Math.min(5, searchRadius));
        int limit = maxBlocks < 0 ? Integer.MAX_VALUE : maxBlocks;
        if (limit == 0) {
            return List.of();
        }

        Set<String> visited = new HashSet<>();
        ArrayDeque<BlockPosition> queue = new ArrayDeque<>();
        List<BlockPosition> result = new ArrayList<>();

        Material originType = safeType(world, origin.x(), origin.y(), origin.z());
        if (originType == null || !include.test(originType)) {
            return result;
        }
        visited.add(key(origin.x(), origin.y(), origin.z()));
        queue.add(origin);

        while (!queue.isEmpty() && result.size() < limit) {
            BlockPosition current = queue.poll();
            Material type = safeType(world, current.x(), current.y(), current.z());
            if (type == null || !include.test(type)) {
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
                        if (result.size() >= limit) {
                            return result;
                        }
                        int nx = cx + dx;
                        int ny = cy + dy;
                        int nz = cz + dz;
                        String nextKey = key(nx, ny, nz);
                        if (!visited.add(nextKey)) {
                            continue;
                        }
                        Material nextType = safeType(world, nx, ny, nz);
                        if (nextType == null || !include.test(nextType)) {
                            continue;
                        }
                        BlockPosition next = new BlockPosition(world, nx, ny, nz);
                        if (isTrunkMaterial(nextType)) {
                            queue.addFirst(next);
                        } else {
                            queue.addLast(next);
                        }
                    }
                }
            }
        }
        return result;
    }

    public static boolean isTrunkMaterial(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_LOG")
                || name.endsWith("_WOOD")
                || name.endsWith("_STEM")
                || name.endsWith("_HYPHAE");
    }

    public static Material safeType(World world, int x, int y, int z) {
        if (world == null) {
            return null;
        }
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return null;
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return null;
        }
        if (!Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ)) {
            return null;
        }
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        return chunk.getBlock(x & 15, y, z & 15).getType();
    }

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
}
