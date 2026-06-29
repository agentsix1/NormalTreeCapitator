package dev.normaltreecapitator.scheduler;

import dev.normaltreecapitator.util.ServerPlatform;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Unified scheduling for Paper/Folia with Spigot/Bukkit fallback.
 */
public final class PluginScheduler {

    private final JavaPlugin plugin;
    private final boolean paperSchedulers;
    private RegionScheduler region;
    private AsyncScheduler async;

    public PluginScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
        boolean paper = ServerPlatform.isPaper();
        if (paper) {
            var server = plugin.getServer();
            this.region = server.getRegionScheduler();
            this.async = server.getAsyncScheduler();
        }
        this.paperSchedulers = paper;
    }

    public void runAtLocation(Location location, Runnable task) {
        if (paperSchedulers) {
            region.run(plugin, location, t -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runAtLocationLater(Location location, Runnable task, long delayTicks) {
        if (delayTicks <= 0L) {
            runAtLocation(location, task);
            return;
        }
        if (paperSchedulers) {
            region.runDelayed(plugin, location, t -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public void runOnEntity(Entity entity, Runnable task) {
        if (paperSchedulers) {
            EntityScheduler scheduler = entity.getScheduler();
            scheduler.run(plugin, t -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runOnEntityLater(Entity entity, Runnable task, long delayTicks) {
        if (paperSchedulers) {
            entity.getScheduler().runDelayed(plugin, t -> task.run(), null, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public void runAsync(Runnable task) {
        if (paperSchedulers) {
            async.runNow(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public boolean isFolia() {
        return ServerPlatform.isFolia();
    }
}
