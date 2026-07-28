package dev.normaltreecapitator.session;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks in-progress tree-cap chains so {@code /tc status} can report progress and ETA.
 */
public final class ChainProgressTracker {

    private final ConcurrentHashMap<UUID, ChainJob> jobs = new ConcurrentHashMap<>();

    public void start(
            UUID playerId,
            int total,
            boolean async,
            int blocksPerTick,
            int asyncDelayTicks
    ) {
        if (playerId == null || total <= 0) {
            return;
        }
        jobs.put(playerId, new ChainJob(
                total,
                new AtomicInteger(0),
                System.currentTimeMillis(),
                async,
                Math.max(1, blocksPerTick),
                Math.max(0, asyncDelayTicks)
        ));
    }

    public void setCompleted(UUID playerId, int completed) {
        ChainJob job = jobs.get(playerId);
        if (job == null) {
            return;
        }
        job.completed().set(Math.max(0, Math.min(completed, job.total())));
    }

    public void finish(UUID playerId) {
        if (playerId != null) {
            jobs.remove(playerId);
        }
    }

    public Optional<ChainJob> get(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(jobs.get(playerId));
    }

    public record ChainJob(
            int total,
            AtomicInteger completed,
            long startedAtMs,
            boolean async,
            int blocksPerTick,
            int asyncDelayTicks
    ) {
        public int done() {
            return Math.min(Math.max(0, completed.get()), total);
        }

        public int remaining() {
            return Math.max(0, total - done());
        }

        public int percent() {
            if (total <= 0) {
                return 0;
            }
            return (int) Math.min(100, Math.round(100.0 * done() / total));
        }

        /** Rough remaining time in milliseconds, or {@code -1} if not enough data yet. */
        public long estimateRemainingMs() {
            int done = done();
            int left = remaining();
            if (left <= 0) {
                return 0;
            }
            long elapsed = Math.max(1L, System.currentTimeMillis() - startedAtMs);
            if (done > 0) {
                double msPerBlock = (double) elapsed / done;
                return Math.round(msPerBlock * left);
            }
            if (async) {
                int waves = (int) Math.ceil((double) left / blocksPerTick);
                long ticks = (long) waves * Math.max(1, asyncDelayTicks);
                return ticks * 50L;
            }
            return -1L;
        }
    }
}
