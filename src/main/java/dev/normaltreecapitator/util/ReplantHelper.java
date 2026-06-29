package dev.normaltreecapitator.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public final class ReplantHelper {

    public static final String META_INV_REPL = "ntc_inv_repl";

    private static final Map<Material, Material> SAPLING_BY_LOG = buildSaplingMap();

    private ReplantHelper() {
    }

    public static boolean replant(Block logBlock, Material logType, boolean invincibleReplant, Plugin plugin) {
        Material sapling = SAPLING_BY_LOG.get(logType);
        if (sapling == null) {
            return false;
        }
        Block below = logBlock.getWorld().getBlockAt(
                logBlock.getX(), logBlock.getY() - 1, logBlock.getZ()
        );
        if (!canPlantOn(below.getType(), logType)) {
            return false;
        }
        if (!logBlock.breakNaturally()) {
            return false;
        }
        logBlock.setType(sapling);
        if (invincibleReplant) {
            logBlock.setMetadata(META_INV_REPL, new FixedMetadataValue(plugin, true));
            below.setMetadata(META_INV_REPL, new FixedMetadataValue(plugin, true));
        }
        return true;
    }

    private static boolean canPlantOn(Material ground, Material logType) {
        if (logType == Material.MANGROVE_LOG) {
            return ground == Material.DIRT
                    || ground == Material.GRASS_BLOCK
                    || ground == Material.MUD
                    || ground == Material.CLAY;
        }
        if (logType == Material.CRIMSON_STEM) {
            return ground == Material.CRIMSON_NYLIUM;
        }
        if (logType == Material.WARPED_STEM) {
            return ground == Material.WARPED_NYLIUM;
        }
        return ground == Material.DIRT
                || ground == Material.GRASS_BLOCK
                || ground == Material.PODZOL
                || ground == Material.COARSE_DIRT
                || ground == Material.ROOTED_DIRT
                || ground == Material.MOSS_BLOCK
                || ground == Material.MYCELIUM
                || ground == Material.FARMLAND;
    }

    private static Map<Material, Material> buildSaplingMap() {
        Map<Material, Material> map = new HashMap<>();
        putSapling(map, Material.OAK_LOG, Material.OAK_SAPLING);
        putSapling(map, Material.SPRUCE_LOG, Material.SPRUCE_SAPLING);
        putSapling(map, Material.BIRCH_LOG, Material.BIRCH_SAPLING);
        putSapling(map, Material.JUNGLE_LOG, Material.JUNGLE_SAPLING);
        putSapling(map, Material.ACACIA_LOG, Material.ACACIA_SAPLING);
        putSapling(map, Material.DARK_OAK_LOG, Material.DARK_OAK_SAPLING);
        putSapling(map, Material.MANGROVE_LOG, Material.MANGROVE_PROPAGULE);
        putSapling(map, Material.CHERRY_LOG, Material.CHERRY_SAPLING);
        Material paleOakLog = Material.matchMaterial("PALE_OAK_LOG");
        Material paleOakSapling = Material.matchMaterial("PALE_OAK_SAPLING");
        if (paleOakLog != null && paleOakSapling != null) {
            putSapling(map, paleOakLog, paleOakSapling);
        }
        putSapling(map, Material.CRIMSON_STEM, Material.CRIMSON_FUNGUS);
        putSapling(map, Material.WARPED_STEM, Material.WARPED_FUNGUS);
        return Map.copyOf(map);
    }

    private static void putSapling(Map<Material, Material> map, Material log, Material sapling) {
        try {
            map.put(log, sapling);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
