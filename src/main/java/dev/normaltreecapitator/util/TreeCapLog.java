package dev.normaltreecapitator.util;

import dev.normaltreecapitator.config.TreeCapitatorConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Tree capitator console diagnostics.
 * Only prints when {@code settings.debug} is true in {@code config.yml}.
 */
public final class TreeCapLog {

    private TreeCapLog() {
    }

    private static Logger logger(Plugin plugin) {
        if (plugin != null) {
            return plugin.getLogger();
        }
        Plugin ntc = Bukkit.getPluginManager().getPlugin("NormalTreeCapitator");
        return ntc != null ? ntc.getLogger() : Bukkit.getLogger();
    }

    public static void info(TreeCapitatorConfig config, Plugin plugin, String message) {
        if (config == null || !config.debug()) {
            return;
        }
        logger(plugin).info("[TreeCap] " + message);
    }

    public static void info(TreeCapitatorConfig config, Plugin plugin, Player player, String message) {
        if (config == null || !config.debug()) {
            return;
        }
        String name = player == null ? "?" : player.getName();
        logger(plugin).info("[TreeCap] (" + name + ") " + message);
    }

    public static String blockLabel(Location location, Material type) {
        if (location == null) {
            return String.valueOf(type);
        }
        String world = location.getWorld() == null ? "?" : location.getWorld().getName();
        return type + " @ " + world + " "
                + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    public static String formatDrops(Iterable<ItemStack> drops) {
        if (drops == null) {
            return "drops=[]";
        }
        Map<Material, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : drops) {
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }
            counts.merge(stack.getType(), stack.getAmount(), Integer::sum);
        }
        if (counts.isEmpty()) {
            return "drops=[]";
        }
        StringBuilder out = new StringBuilder("drops=[");
        boolean first = true;
        for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
            if (!first) {
                out.append(", ");
            }
            out.append(entry.getKey().name()).append('x').append(entry.getValue());
            first = false;
        }
        return out.append(']').toString();
    }

    public static String saplingSummary(BulkDropAccumulator accumulator) {
        if (accumulator == null) {
            return "pool=none";
        }
        return "pool=" + accumulator.debugSaplingCounts();
    }

    public static String connectedRemaining(
            Location at,
            List<BlockPosition> chain,
            Predicate<Material> isTreeBlock
    ) {
        if (at == null || at.getWorld() == null || chain == null || chain.isEmpty()) {
            return "connectedRemaining=0";
        }
        int ax = at.getBlockX();
        int ay = at.getBlockY();
        int az = at.getBlockZ();
        int remaining = 0;
        StringBuilder samples = new StringBuilder();
        int samplesLogged = 0;
        for (BlockPosition pos : chain) {
            if (pos.world() != at.getWorld()) {
                continue;
            }
            int dx = Math.abs(pos.x() - ax);
            int dy = Math.abs(pos.y() - ay);
            int dz = Math.abs(pos.z() - az);
            if (dx > 1 || dy > 1 || dz > 1 || (dx == 0 && dy == 0 && dz == 0)) {
                continue;
            }
            Material type = AdjacentFlooder.safeType(pos.world(), pos.x(), pos.y(), pos.z());
            if (type == null || type.isAir() || (isTreeBlock != null && !isTreeBlock.test(type))) {
                continue;
            }
            remaining++;
            if (samplesLogged < 4) {
                if (samplesLogged > 0) {
                    samples.append(", ");
                }
                samples.append(type.name())
                        .append('@')
                        .append(pos.x()).append(',').append(pos.y()).append(',').append(pos.z());
                samplesLogged++;
            }
        }
        if (remaining == 0) {
            return "connectedRemaining=0";
        }
        return "connectedRemaining=" + remaining + " [" + samples
                + (remaining > samplesLogged ? ", ..." : "")
                + "]";
    }

    public static String chainSummary(List<BlockPosition> chain) {
        if (chain == null || chain.isEmpty()) {
            return "chain=[]";
        }
        Map<Material, Integer> counts = new LinkedHashMap<>();
        for (BlockPosition pos : chain) {
            Material type = AdjacentFlooder.safeType(pos.world(), pos.x(), pos.y(), pos.z());
            if (type == null) {
                continue;
            }
            counts.merge(type, 1, Integer::sum);
        }
        StringBuilder out = new StringBuilder("chainTypes=[");
        boolean first = true;
        for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
            if (!first) {
                out.append(", ");
            }
            out.append(entry.getKey()).append('x').append(entry.getValue());
            first = false;
        }
        return out.append("] size=").append(chain.size()).toString();
    }

    public static String replantSitesSummary(Collection<PendingReplant> sites) {
        if (sites == null || sites.isEmpty()) {
            return "replantSites=0 []";
        }
        StringBuilder out = new StringBuilder("replantSites=").append(sites.size()).append(" [");
        int i = 0;
        for (PendingReplant site : sites) {
            if (i > 0) {
                out.append("; ");
            }
            Material sapling = site.expectedSapling() != null
                    ? site.expectedSapling()
                    : TreeReplant.saplingForLog(site.logType());
            out.append(blockLabel(site.location(), site.logType()))
                    .append(" log=")
                    .append(site.logType())
                    .append(" -> ")
                    .append(sapling == null ? "?" : sapling);
            i++;
            if (i >= 8 && sites.size() > 8) {
                out.append("; ... +").append(sites.size() - 8).append(" more");
                break;
            }
        }
        return out.append(']').toString();
    }

    public static String brokenLogsSummary(Collection<PendingReplant> brokenLogs) {
        if (brokenLogs == null || brokenLogs.isEmpty()) {
            return "brokenLogs=0 []";
        }
        StringBuilder out = new StringBuilder("brokenLogs=").append(brokenLogs.size()).append(" [");
        int i = 0;
        for (PendingReplant log : brokenLogs) {
            if (i > 0) {
                out.append("; ");
            }
            Material sapling = log.expectedSapling() != null
                    ? log.expectedSapling()
                    : TreeReplant.saplingForLog(log.logType());
            out.append(blockLabel(log.location(), log.logType()))
                    .append(" log=")
                    .append(log.logType())
                    .append(" ground=")
                    .append(log.plantableGround())
                    .append(" -> ")
                    .append(sapling == null ? "?" : sapling);
            i++;
            if (i >= 12 && brokenLogs.size() > 12) {
                out.append("; ... +").append(brokenLogs.size() - 12).append(" more");
                break;
            }
        }
        return out.append(']').toString();
    }
}
