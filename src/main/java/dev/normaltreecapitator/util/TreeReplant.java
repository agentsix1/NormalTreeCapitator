package dev.normaltreecapitator.util;

import dev.normaltreecapitator.NormalTreeCapitator;
import dev.normaltreecapitator.config.TreeCapitatorConfig;
import dev.normaltreecapitator.scheduler.PluginScheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class TreeReplant {

    public static final String META_INV_REPL = "ntc_inv_repl";

    private static final Map<Material, Material> SAPLING_BY_LOG = buildSaplingMap();

    private TreeReplant() {
    }

    public static PendingReplant recordBrokenLog(Location location, Material logType, boolean plantableGround) {
        Material sapling = saplingForLog(logType);
        Location at = location == null ? null : location.clone();
        return new PendingReplant(at, logType, plantableGround, sapling);
    }

    public static List<PendingReplant> stumpsFromBrokenLogs(List<PendingReplant> brokenLogs) {
        return findStumpSitesFromBrokenLogs(brokenLogs);
    }

    public static Material saplingForLog(Material logType) {
        if (logType == null) {
            return null;
        }
        Material direct = SAPLING_BY_LOG.get(logType);
        if (direct != null) {
            return direct;
        }
        String name = logType.name();
        if (name.startsWith("STRIPPED_")) {
            name = name.substring("STRIPPED_".length());
        }
        if (name.endsWith("_WOOD")) {
            name = name.substring(0, name.length() - "_WOOD".length()) + "_LOG";
        }
        if (name.endsWith("_HYPHAE")) {
            name = name.substring(0, name.length() - "_HYPHAE".length()) + "_STEM";
        }
        try {
            return SAPLING_BY_LOG.get(Material.valueOf(name));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String treeFamily(Material material) {
        if (material == null) {
            return "";
        }
        String name = material.name();
        if (name.startsWith("DARK_OAK")) {
            return "DARK_OAK";
        }
        if (name.startsWith("PALE_OAK")) {
            return "PALE_OAK";
        }
        if (name.contains("CRIMSON") || name.equals("NETHER_WART_BLOCK")) {
            return "CRIMSON";
        }
        if (name.contains("WARPED") || name.equals("WARPED_WART_BLOCK")) {
            return "WARPED";
        }
        if (name.equals("SHROOMLIGHT")) {
            return "NETHER_SHARED";
        }
        for (String prefix : List.of(
                "OAK", "BIRCH", "SPRUCE", "JUNGLE", "ACACIA", "CHERRY", "MANGROVE"
        )) {
            if (name.startsWith(prefix + "_")) {
                return prefix;
            }
        }
        return name;
    }

    public static boolean sameTreeFamily(String originFamily, Material candidate) {
        if (originFamily == null || originFamily.isEmpty() || candidate == null) {
            return false;
        }
        String family = treeFamily(candidate);
        if (originFamily.equals(family)) {
            return true;
        }
        if ("NETHER_SHARED".equals(family)
                && ("CRIMSON".equals(originFamily) || "WARPED".equals(originFamily))) {
            return true;
        }
        if ("NETHER_SHARED".equals(originFamily)
                && ("CRIMSON".equals(family) || "WARPED".equals(family))) {
            return true;
        }
        return false;
    }

    public static List<PendingReplant> findStumpSites(List<BlockPosition> chain) {
        if (chain == null || chain.isEmpty()) {
            return List.of();
        }
        List<PendingReplant> logs = new ArrayList<>();
        for (BlockPosition pos : chain) {
            if (pos.world() == null) {
                continue;
            }
            Material type = AdjacentFlooder.safeType(pos.world(), pos.x(), pos.y(), pos.z());
            if (type == null || !isReplantableLogType(type)) {
                continue;
            }
            Material below = AdjacentFlooder.safeType(pos.world(), pos.x(), pos.y() - 1, pos.z());
            boolean plantable = below != null && canPlantOn(below, type);
            Location loc = new Location(pos.world(), pos.x(), pos.y(), pos.z());
            logs.add(recordBrokenLog(loc, type, plantable));
        }
        return findStumpSitesFromBrokenLogs(logs);
    }

    public static List<PendingReplant> findStumpSitesFromBrokenLogs(List<PendingReplant> brokenLogs) {
        if (brokenLogs == null || brokenLogs.isEmpty()) {
            return List.of();
        }
        Map<String, PendingReplant> lowest = new HashMap<>();
        Set<String> logColumns = new HashSet<>();
        for (PendingReplant log : brokenLogs) {
            if (log == null || log.location() == null || log.location().getWorld() == null) {
                continue;
            }
            if (!isReplantableLogType(log.logType())) {
                continue;
            }
            String key = columnKey(log.location());
            logColumns.add(key);
            PendingReplant existing = lowest.get(key);
            if (existing == null || log.location().getBlockY() < existing.location().getBlockY()) {
                lowest.put(key, log);
            }
        }
        Map<String, PendingReplant> stumps = new LinkedHashMap<>();
        for (Map.Entry<String, PendingReplant> entry : lowest.entrySet()) {
            PendingReplant log = entry.getValue();
            if (log.plantableGround() || hasPlantableGround(log.location().getBlock(), log.logType())) {
                stumps.put(entry.getKey(), log);
            }
        }
        expandTwoByTwoStumps(stumps, lowest, logColumns);
        return List.copyOf(stumps.values());
    }

    private static void expandTwoByTwoStumps(
            Map<String, PendingReplant> stumps,
            Map<String, PendingReplant> lowest,
            Set<String> logColumns
    ) {
        List<PendingReplant> snapshot = new ArrayList<>(stumps.values());
        for (PendingReplant stump : snapshot) {
            Location loc = stump.location();
            if (loc.getWorld() == null) {
                continue;
            }
            String world = loc.getWorld().getName();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            Material type = stump.logType();
            tryAddTwoByTwoCorner(stumps, lowest, logColumns, world, x, y, z, type, 1, 1);
            tryAddTwoByTwoCorner(stumps, lowest, logColumns, world, x, y, z, type, 1, -1);
            tryAddTwoByTwoCorner(stumps, lowest, logColumns, world, x, y, z, type, -1, 1);
            tryAddTwoByTwoCorner(stumps, lowest, logColumns, world, x, y, z, type, -1, -1);
        }
    }

    private static void tryAddTwoByTwoCorner(
            Map<String, PendingReplant> stumps,
            Map<String, PendingReplant> lowest,
            Set<String> logColumns,
            String world,
            int x,
            int y,
            int z,
            Material type,
            int dx,
            int dz
    ) {
        int[][] corners = {
                {x + dx, z},
                {x, z + dz},
                {x + dx, z + dz}
        };
        int present = 0;
        for (int[] corner : corners) {
            if (logColumns.contains(world + "|" + corner[0] + "|" + corner[1])) {
                present++;
            }
        }
        if (present < 1) {
            return;
        }
        for (int[] corner : corners) {
            String key = world + "|" + corner[0] + "|" + corner[1];
            if (stumps.containsKey(key) || !logColumns.contains(key)) {
                continue;
            }
            PendingReplant log = lowest.get(key);
            if (log == null || log.location().getBlockY() != y) {
                continue;
            }
            if (!log.plantableGround() && !hasPlantableGround(log.location().getBlock(), log.logType())) {
                continue;
            }
            if (!sameTreeFamily(treeFamily(type), log.logType())) {
                continue;
            }
            stumps.put(key, log);
        }
    }

    private static String columnKey(Location loc) {
        return loc.getWorld().getName() + "|" + loc.getBlockX() + "|" + loc.getBlockZ();
    }

    public static boolean hasPlantableGround(Block logBlock, Material logType) {
        if (logBlock == null || logType == null || saplingForLog(logType) == null) {
            return false;
        }
        Block below = logBlock.getWorld().getBlockAt(
                logBlock.getX(), logBlock.getY() - 1, logBlock.getZ()
        );
        return canPlantOn(below.getType(), logType);
    }

    public static boolean isReplantableLogType(Material material) {
        return saplingForLog(material) != null;
    }

    public static void applyReplantsAfterBreak(
            Player player,
            Iterable<PendingReplant> pendingReplants,
            BulkDropAccumulator accumulator,
            boolean consumeFromDrops,
            boolean invincibleReplant,
            Plugin plugin,
            PluginScheduler scheduler,
            TreeCapitatorConfig config,
            Runnable onComplete
    ) {
        Runnable done = onComplete == null ? () -> {
        } : onComplete;
        if (pendingReplants == null || scheduler == null) {
            TreeCapLog.info(config, plugin, player, "replant pass aborted (null pending/scheduler)");
            done.run();
            return;
        }

        List<PendingReplant> sites = new ArrayList<>();
        for (PendingReplant pending : pendingReplants) {
            if (pending != null && pending.location() != null && pending.location().getWorld() != null) {
                sites.add(pending);
            }
        }
        TreeCapLog.info(config, plugin, player,
                "REPLANT PROCESS BEGIN sites=" + sites.size()
                        + " consume=" + consumeFromDrops
                        + " " + TreeCapLog.replantSitesSummary(sites)
                        + " " + TreeCapLog.saplingSummary(accumulator));
        if (sites.isEmpty()) {
            TreeCapLog.info(config, plugin, player, "REPLANT PROCESS: no blocks to replant");
            done.run();
            return;
        }
        sites.sort(Comparator.comparingInt(site -> site.location().getBlockY()));

        AtomicInteger remaining = new AtomicInteger(sites.size());
        AtomicInteger planted = new AtomicInteger();
        AtomicInteger siteSeq = new AtomicInteger();
        int totalSites = sites.size();
        for (PendingReplant pending : sites) {
            scheduler.runAtLocation(pending.location(), () -> {
                try {
                    int n = siteSeq.incrementAndGet();
                    if (tryPlantStump(
                            player,
                            pending,
                            accumulator,
                            consumeFromDrops,
                            invincibleReplant,
                            plugin,
                            config,
                            n,
                            totalSites
                    )) {
                        planted.incrementAndGet();
                    }
                } finally {
                    if (remaining.decrementAndGet() == 0) {
                        TreeCapLog.info(config, plugin, player,
                                "REPLANT PROCESS DONE planted=" + planted.get()
                                        + "/" + sites.size()
                                        + " " + TreeCapLog.saplingSummary(accumulator));
                        done.run();
                    }
                }
            });
        }
    }

    private static boolean tryPlantStump(
            Player player,
            PendingReplant pending,
            BulkDropAccumulator accumulator,
            boolean consumeFromDrops,
            boolean invincibleReplant,
            Plugin plugin,
            TreeCapitatorConfig config,
            int siteIndex,
            int totalSites
    ) {
        Material sapling = pending.expectedSapling() != null
                ? pending.expectedSapling()
                : saplingForLog(pending.logType());
        String where = TreeCapLog.blockLabel(pending.location(), pending.logType());
        String prefix = "REPLANT " + siteIndex + "/" + totalSites + " ";
        TreeCapLog.info(config, plugin, player,
                prefix + "ATTEMPT at " + where
                        + " logWas=" + pending.logType()
                        + " replantAs=" + sapling
                        + " plantableGround=" + pending.plantableGround()
                        + " consume=" + consumeFromDrops
                        + " sneaking=" + (player != null && player.isSneaking()));
        if (sapling == null) {
            TreeCapLog.info(config, plugin, player, prefix + "RESULT=SKIP reason=no-sapling-map at " + where);
            return false;
        }
        Block block = pending.location().getBlock();
        Block below = block.getRelative(org.bukkit.block.BlockFace.DOWN);

        if (!pending.plantableGround()) {
            if (!canPlantOn(below.getType(), pending.logType())) {
                TreeCapLog.info(config, plugin, player,
                        prefix + "RESULT=SKIP reason=bad-ground below=" + below.getType() + " at " + where);
                return false;
            }
        }

        if (consumeFromDrops) {
            int available = accumulator == null ? 0 : accumulator.countOf(sapling);
            if (accumulator == null || !accumulator.tryConsumeSapling(sapling)) {
                TreeCapLog.info(config, plugin, player,
                        prefix + "RESULT=SKIP reason=no-sapling-in-drops need=" + sapling
                                + " available=" + available
                                + " at " + where
                                + " " + TreeCapLog.saplingSummary(accumulator));
                return false;
            }
        }

        Material current = block.getType();
        if (!current.isAir() && current != sapling) {
            String name = current.name();
            boolean treeLeftover = name.endsWith("_LOG")
                    || name.endsWith("_WOOD")
                    || name.endsWith("_STEM")
                    || name.endsWith("_HYPHAE")
                    || name.contains("LEAVES")
                    || name.contains("WART_BLOCK")
                    || name.equals("SHROOMLIGHT");
            if (treeLeftover || !current.isSolid()) {
                block.setType(Material.AIR, false);
            } else {
                if (consumeFromDrops) {
                    accumulator.addDrops(List.of(new ItemStack(sapling, 1)));
                }
                TreeCapLog.info(config, plugin, player,
                        prefix + "RESULT=SKIP reason=not-air current=" + current + " at " + where);
                return false;
            }
        }

        if (!placeSapling(block, below, sapling, invincibleReplant, plugin)) {
            if (consumeFromDrops) {
                accumulator.addDrops(List.of(new ItemStack(sapling, 1)));
            }
            TreeCapLog.info(config, plugin, player,
                    prefix + "RESULT=FAIL setType wanted=" + sapling
                            + " got=" + block.getType() + " at " + where);
            return false;
        }
        TreeCapLog.info(config, plugin, player,
                prefix + "RESULT=PLANTED " + sapling
                        + (consumeFromDrops ? "" : " free")
                        + " at " + where
                        + " " + TreeCapLog.saplingSummary(accumulator));
        return true;
    }

    private static boolean placeSapling(
            Block logBlock,
            Block below,
            Material sapling,
            boolean invincibleReplant,
            Plugin plugin
    ) {
        applySaplingBlock(logBlock, sapling);
        if (invincibleReplant) {
            logBlock.setMetadata(META_INV_REPL, new FixedMetadataValue(plugin, true));
            below.setMetadata(META_INV_REPL, new FixedMetadataValue(plugin, true));
        }
        scheduleSaplingConfirm(plugin, logBlock, below, sapling, invincibleReplant);
        return logBlock.getType() == sapling;
    }

    private static void applySaplingBlock(Block logBlock, Material sapling) {
        logBlock.setType(sapling, false);
    }

    private static void scheduleSaplingConfirm(
            Plugin plugin,
            Block logBlock,
            Block below,
            Material sapling,
            boolean invincibleReplant
    ) {
        if (!(plugin instanceof NormalTreeCapitator ntc)) {
            return;
        }
        Location at = logBlock.getLocation();
        ntc.getScheduler().runAtLocationLater(at, () -> {
            Material type = logBlock.getType();
            if (!type.isAir() && type != sapling) {
                return;
            }
            if (type != sapling) {
                applySaplingBlock(logBlock, sapling);
            }
            if (invincibleReplant && logBlock.getType() == sapling) {
                logBlock.setMetadata(META_INV_REPL, new FixedMetadataValue(plugin, true));
                below.setMetadata(META_INV_REPL, new FixedMetadataValue(plugin, true));
            }
        }, 2L);
    }

    private static boolean canPlantOn(Material ground, Material logType) {
        if (ground == null || ground.isAir()) {
            return false;
        }
        Material effectiveLog = logType;
        String name = logType == null ? "" : logType.name();
        if (name.startsWith("STRIPPED_")) {
            name = name.substring("STRIPPED_".length());
            try {
                effectiveLog = Material.valueOf(name.endsWith("_WOOD")
                        ? name.substring(0, name.length() - 5) + "_LOG"
                        : name);
            } catch (IllegalArgumentException ignored) {
            }
        } else if (name.endsWith("_WOOD")) {
            try {
                effectiveLog = Material.valueOf(name.substring(0, name.length() - 5) + "_LOG");
            } catch (IllegalArgumentException ignored) {
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
            return ground == Material.CRIMSON_NYLIUM
                    || ground == Material.NETHERRACK;
        }
        if (effectiveLog == Material.WARPED_STEM) {
            return ground == Material.WARPED_NYLIUM
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
        putSapling(map, Material.PALE_OAK_LOG, Material.PALE_OAK_SAPLING);
        putSapling(map, Material.CRIMSON_STEM, Material.CRIMSON_FUNGUS);
        putSapling(map, Material.WARPED_STEM, Material.WARPED_FUNGUS);
        putSapling(map, Material.OAK_WOOD, Material.OAK_SAPLING);
        putSapling(map, Material.SPRUCE_WOOD, Material.SPRUCE_SAPLING);
        putSapling(map, Material.BIRCH_WOOD, Material.BIRCH_SAPLING);
        putSapling(map, Material.JUNGLE_WOOD, Material.JUNGLE_SAPLING);
        putSapling(map, Material.ACACIA_WOOD, Material.ACACIA_SAPLING);
        putSapling(map, Material.DARK_OAK_WOOD, Material.DARK_OAK_SAPLING);
        putSapling(map, Material.MANGROVE_WOOD, Material.MANGROVE_PROPAGULE);
        putSapling(map, Material.CHERRY_WOOD, Material.CHERRY_SAPLING);
        putSapling(map, Material.PALE_OAK_WOOD, Material.PALE_OAK_SAPLING);
        putSapling(map, Material.CRIMSON_HYPHAE, Material.CRIMSON_FUNGUS);
        putSapling(map, Material.WARPED_HYPHAE, Material.WARPED_FUNGUS);
        return Map.copyOf(map);
    }

    private static void putSapling(Map<Material, Material> map, Material log, Material sapling) {
        if (log != null && sapling != null) {
            map.put(log, sapling);
        }
    }
}
