package dev.normaltreecapitator.session;

import dev.normaltreecapitator.NormalTreeCapitator;
import dev.normaltreecapitator.config.TreeCapitatorConfig;
import dev.normaltreecapitator.util.BulkDropAccumulator;
import dev.normaltreecapitator.util.DropHelper;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks in-progress tree-cap jobs so {@code /tc cancel} / {@code /tc stop} can halt them,
 * flush collected drops, and prevent further axe damage.
 */
public final class ActiveTreeCapJobs {

    private final ConcurrentHashMap<UUID, Job> jobs = new ConcurrentHashMap<>();

    public Job begin(
            UUID playerId,
            BulkDropAccumulator accumulator,
            Location dropAt,
            TreeCapitatorConfig config
    ) {
        Job job = new Job(playerId, accumulator, dropAt.clone(), config);
        jobs.put(playerId, job);
        return job;
    }

    public Optional<Job> get(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(jobs.get(playerId));
    }

    public void end(Job job) {
        if (job != null) {
            jobs.remove(job.playerId, job);
        }
    }

    /**
     * @return {@code true} if an active (not-yet-cancelled) job was cancelled
     */
    public boolean requestCancel(UUID playerId, NormalTreeCapitator plugin) {
        Job job = jobs.get(playerId);
        if (job == null) {
            return false;
        }
        return job.cancel(plugin);
    }

    public static final class Job {
        private final UUID playerId;
        private final BulkDropAccumulator accumulator;
        private final Location dropAt;
        private final TreeCapitatorConfig config;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean dropsReleased = new AtomicBoolean(false);

        private Job(
                UUID playerId,
                BulkDropAccumulator accumulator,
                Location dropAt,
                TreeCapitatorConfig config
        ) {
            this.playerId = playerId;
            this.accumulator = accumulator;
            this.dropAt = dropAt;
            this.config = config;
        }

        public boolean isCancelled() {
            return cancelled.get();
        }

        public boolean dropsAlreadyReleased() {
            return dropsReleased.get();
        }

        boolean cancel(NormalTreeCapitator plugin) {
            if (!cancelled.compareAndSet(false, true)) {
                return false;
            }
            releaseCollectedDrops(plugin);
            plugin.chainProgress().finish(playerId);
            return true;
        }

        /**
         * Called from the natural finish path when the job was cancelled mid-break.
         * Ensures drops were flushed and the job is removed.
         */
        public void completeAfterCancel(NormalTreeCapitator plugin) {
            releaseCollectedDrops(plugin);
            plugin.activeTreeCaps().end(this);
        }

        private void releaseCollectedDrops(NormalTreeCapitator plugin) {
            if (!dropsReleased.compareAndSet(false, true)) {
                return;
            }
            if (!config.mergeItemDrops()) {
                return;
            }
            List<org.bukkit.inventory.ItemStack> drops = accumulator.takeMergedDrops();
            if (drops.isEmpty()) {
                return;
            }
            plugin.getScheduler().runAtLocation(dropAt, () -> DropHelper.dropStacks(dropAt, drops));
        }
    }
}
