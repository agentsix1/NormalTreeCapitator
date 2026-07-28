package dev.normaltreecapitator.util;

import dev.normaltreecapitator.config.TreeBlockGroup;
import dev.normaltreecapitator.config.TreeCapitatorConfig;
import dev.normaltreecapitator.playerdata.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.permissions.Permissible;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Allows tree-cap only when a collected chain looks like a natural tree
 * (grounded stump + real foliage/canopy). Orphan leaf-only / log-only chains
 * can be allowed via {@link #isOrphanCleanupChain} when cleanup is enabled.
 */
public final class StructureProtection {

    private static final int[][] FACE_OFFSETS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    private StructureProtection() {
    }

    /**
     * Whether structure protection should run for this player right now.
     * Players with {@code normaltreecapitator.structure-protection} may opt out
     * via {@code /tc structure-protection}.
     */
    public static boolean appliesTo(Permissible player, PlayerData data, TreeCapitatorConfig config) {
        if (config == null || !config.structureProtection()) {
            return false;
        }
        if (data != null
                && !data.structureProtection()
                && player != null
                && player.hasPermission("normaltreecapitator.structure-protection")) {
            return false;
        }
        return true;
    }

    /**
     * @param chainCapped {@code true} if flood-fill stopped at max-chain (foliage may be missing)
     * @return reason string if the chain should not be tree-capped; {@code null} if OK
     */
    public static String findStructureReason(
            List<BlockPosition> chain,
            TreeBlockGroup group,
            TreeCapitatorConfig config,
            boolean chainCapped
    ) {
        if (!config.structureProtection() || chain == null || chain.isEmpty()) {
            return null;
        }
        if (group == null) {
            return null;
        }

        int trunkCount = 0;
        int foliageCount = 0;
        int leafBlocks = 0;
        int persistentLeaves = 0;
        int naturalLeaves = 0;
        int lowestTrunkY = Integer.MAX_VALUE;
        int highestFoliageY = Integer.MIN_VALUE;
        Map<String, Integer> lowestTrunkByColumn = new HashMap<>();
        Map<String, Material> lowestTrunkType = new HashMap<>();

        for (BlockPosition pos : chain) {
            Material type = AdjacentFlooder.safeType(pos.world(), pos.x(), pos.y(), pos.z());
            if (type == null) {
                continue;
            }
            if (AdjacentFlooder.isTrunkMaterial(type)) {
                trunkCount++;
                lowestTrunkY = Math.min(lowestTrunkY, pos.y());
                String column = pos.x() + "|" + pos.z();
                Integer prev = lowestTrunkByColumn.get(column);
                if (prev == null || pos.y() < prev) {
                    lowestTrunkByColumn.put(column, pos.y());
                    lowestTrunkType.put(column, type);
                }
            }
            if (isFoliage(type)) {
                foliageCount++;
                highestFoliageY = Math.max(highestFoliageY, pos.y());
                if (type.name().endsWith("_LEAVES")) {
                    leafBlocks++;
                    if (isPersistentLeaf(pos)) {
                        persistentLeaves++;
                    } else {
                        naturalLeaves++;
                    }
                }
            }
        }

        if (trunkCount == 0) {
            return foliageCount > 0 ? "foliage-only" : "empty-chain";
        }

        if (foliageCount == 0 && !chainCapped) {
            return "no-foliage";
        }

        if (leafBlocks > 0 && persistentLeaves > naturalLeaves) {
            return "persistent-leaves";
        }

        if (!hasGroundedStump(chain, lowestTrunkByColumn, lowestTrunkType)) {
            return "not-grounded";
        }

        if (!chainCapped && foliageCount > 0 && highestFoliageY <= lowestTrunkY) {
            return "no-canopy";
        }

        return null;
    }

    /**
     * Orphan cleanup: leaf-only clusters, or log-only stacks that only touch
     * other logs in the chain / air / the floor beneath them.
     */
    public static boolean isOrphanCleanupChain(List<BlockPosition> chain, TreeBlockGroup group) {
        if (chain == null || chain.isEmpty() || group == null) {
            return false;
        }

        Set<BlockPosition> inChain = new HashSet<>(chain);
        int trunkCount = 0;
        int foliageCount = 0;

        for (BlockPosition pos : chain) {
            Material type = AdjacentFlooder.safeType(pos.world(), pos.x(), pos.y(), pos.z());
            if (type == null) {
                continue;
            }
            if (AdjacentFlooder.isTrunkMaterial(type)) {
                trunkCount++;
            } else if (isFoliage(type)) {
                foliageCount++;
            }
        }

        if (trunkCount == 0 && foliageCount > 0) {
            return true;
        }
        if (trunkCount > 0 && foliageCount == 0) {
            return isIsolatedLogStack(chain, inChain, group);
        }
        return false;
    }

    private static boolean isIsolatedLogStack(
            List<BlockPosition> chain,
            Set<BlockPosition> inChain,
            TreeBlockGroup group
    ) {
        World world = chain.get(0).world();
        if (world == null) {
            return false;
        }
        boolean anyGrounded = false;
        for (BlockPosition pos : chain) {
            Material type = AdjacentFlooder.safeType(world, pos.x(), pos.y(), pos.z());
            if (type == null || !AdjacentFlooder.isTrunkMaterial(type)) {
                continue;
            }
            for (int[] offset : FACE_OFFSETS) {
                int nx = pos.x() + offset[0];
                int ny = pos.y() + offset[1];
                int nz = pos.z() + offset[2];
                Material neighbor = AdjacentFlooder.safeType(world, nx, ny, nz);
                if (neighbor == null || isPassable(neighbor)) {
                    continue;
                }
                BlockPosition nPos = new BlockPosition(world, nx, ny, nz);
                if (inChain.contains(nPos) || group.matchesBlock(neighbor)) {
                    continue;
                }
                // Floor under the log is allowed; anything else sideways/above is not.
                if (offset[0] == 0 && offset[1] == -1 && offset[2] == 0
                        && isNaturalGround(neighbor, type)) {
                    anyGrounded = true;
                    continue;
                }
                return false;
            }
            Material below = AdjacentFlooder.safeType(world, pos.x(), pos.y() - 1, pos.z());
            if (below != null && isNaturalGround(below, type)) {
                anyGrounded = true;
            }
        }
        return anyGrounded;
    }

    private static boolean isPassable(Material material) {
        return material.isAir()
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR
                || material == Material.WATER
                || material == Material.LAVA;
    }

    private static boolean hasGroundedStump(
            List<BlockPosition> chain,
            Map<String, Integer> lowestTrunkByColumn,
            Map<String, Material> lowestTrunkType
    ) {
        if (lowestTrunkByColumn.isEmpty()) {
            return false;
        }
        World world = chain.get(0).world();
        if (world == null) {
            return false;
        }
        for (Map.Entry<String, Integer> entry : lowestTrunkByColumn.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            int x = Integer.parseInt(parts[0]);
            int z = Integer.parseInt(parts[1]);
            int y = entry.getValue();
            Material logType = lowestTrunkType.get(entry.getKey());
            Material ground = AdjacentFlooder.safeType(world, x, y - 1, z);
            if (ground != null && isNaturalGround(ground, logType)) {
                return true;
            }
        }
        return false;
    }

    static boolean isNaturalGround(Material ground, Material logType) {
        if (ground == null || ground.isAir()) {
            return false;
        }
        if (isPlantableGround(ground, logType)) {
            return true;
        }
        String name = ground.name();
        return switch (ground) {
            case STONE, GRANITE, DIORITE, ANDESITE, DEEPSLATE, TUFF, CALCITE,
                 COBBLESTONE, MOSSY_COBBLESTONE, COBBLED_DEEPSLATE,
                 SAND, RED_SAND, GRAVEL, SNOW_BLOCK, CLAY,
                 NETHERRACK, CRIMSON_NYLIUM, WARPED_NYLIUM,
                 SOUL_SAND, SOUL_SOIL, BASALT, BLACKSTONE,
                 END_STONE, TERRACOTTA -> true;
            default -> name.equals("PALE_MOSS_BLOCK")
                    || name.endsWith("_ORE")
                    || (name.startsWith("DEEPSLATE_") && name.endsWith("_ORE"));
        };
    }

    private static boolean isPlantableGround(Material ground, Material logType) {
        if (ground == null || logType == null) {
            return false;
        }
        Material effectiveLog = logType;
        String name = logType.name();
        if (name.startsWith("STRIPPED_")) {
            name = name.substring("STRIPPED_".length());
            try {
                effectiveLog = Material.valueOf(name.endsWith("_WOOD")
                        ? name.substring(0, name.length() - 5) + "_LOG"
                        : name.endsWith("_HYPHAE")
                        ? name.substring(0, name.length() - 7) + "_STEM"
                        : name);
            } catch (IllegalArgumentException ignored) {
                effectiveLog = logType;
            }
        } else if (name.endsWith("_WOOD")) {
            try {
                effectiveLog = Material.valueOf(name.substring(0, name.length() - 5) + "_LOG");
            } catch (IllegalArgumentException ignored) {
                effectiveLog = logType;
            }
        } else if (name.endsWith("_HYPHAE")) {
            try {
                effectiveLog = Material.valueOf(name.substring(0, name.length() - 7) + "_STEM");
            } catch (IllegalArgumentException ignored) {
                effectiveLog = logType;
            }
        }

        if (effectiveLog == Material.MANGROVE_LOG) {
            return ground == Material.DIRT
                    || ground == Material.GRASS_BLOCK
                    || ground == Material.MUD
                    || ground == Material.CLAY
                    || ground == Material.MUDDY_MANGROVE_ROOTS;
        }
        if (effectiveLog == Material.CRIMSON_STEM) {
            return ground == Material.CRIMSON_NYLIUM || ground == Material.NETHERRACK;
        }
        if (effectiveLog == Material.WARPED_STEM) {
            return ground == Material.WARPED_NYLIUM || ground == Material.NETHERRACK;
        }
        if (name.contains("MUSHROOM")) {
            return ground == Material.DIRT
                    || ground == Material.GRASS_BLOCK
                    || ground == Material.MYCELIUM
                    || ground == Material.PODZOL
                    || ground == Material.COARSE_DIRT
                    || ground == Material.ROOTED_DIRT
                    || ground == Material.NETHERRACK;
        }
        return ground == Material.DIRT
                || ground == Material.GRASS_BLOCK
                || ground == Material.PODZOL
                || ground == Material.COARSE_DIRT
                || ground == Material.ROOTED_DIRT
                || ground == Material.MOSS_BLOCK
                || ground == Material.MYCELIUM
                || ground == Material.FARMLAND
                || ground == Material.DIRT_PATH
                || ground == Material.MUD
                || ground.name().equals("PALE_MOSS_BLOCK");
    }

    static boolean isFoliage(Material type) {
        String name = type.name();
        return name.endsWith("_LEAVES")
                || name.equals("NETHER_WART_BLOCK")
                || name.equals("WARPED_WART_BLOCK")
                || name.equals("SHROOMLIGHT")
                || name.contains("MUSHROOM_BLOCK");
    }

    private static boolean isPersistentLeaf(BlockPosition pos) {
        Block block = safeBlock(pos.world(), pos.x(), pos.y(), pos.z());
        if (block == null) {
            return false;
        }
        BlockData data = block.getBlockData();
        return data instanceof Leaves leaves && leaves.isPersistent();
    }

    private static Block safeBlock(World world, int x, int y, int z) {
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
        return chunk.getBlock(x & 15, y, z & 15);
    }
}
