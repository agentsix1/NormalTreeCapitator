package dev.normaltreecapitator.util;

import dev.normaltreecapitator.NormalTreeCapitator;
import dev.normaltreecapitator.config.TreeBlockGroup;
import dev.normaltreecapitator.config.TreeCapitatorConfig;
import dev.normaltreecapitator.session.ActiveTreeCapJobs;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Breaks large tree chains in timed waves, merging drops at the origin when configured.
 */
public final class BulkBreakExecutor {

    private final NormalTreeCapitator plugin;

    public BulkBreakExecutor(NormalTreeCapitator plugin) {
        this.plugin = plugin;
    }

    public void execute(
            Player player,
            ItemStack tool,
            TreeBlockGroup group,
            TreeCapitatorConfig config,
            Location origin,
            List<BlockPosition> targets
    ) {
        if (targets.isEmpty()) {
            return;
        }

        Location dropAt = origin.clone().add(0.5, 0.5, 0.5);

        plugin.getScheduler().runOnEntity(player, () -> {
            if (!player.isOnline()) {
                TreeCapLog.info(config, plugin, player, "async chain aborted: player offline");
                return;
            }

            TreeCapLog.info(config, plugin, player,
                    "async chain begin targets=" + targets.size()
                            + " origin=" + TreeCapLog.blockLabel(origin, origin.getBlock().getType()));

            plugin.chainProgress().start(
                    player.getUniqueId(),
                    targets.size(),
                    true,
                    config.blocksPerTick(),
                    config.asyncDelay()
            );

            AtomicBoolean toolBroken = new AtomicBoolean(false);
            BulkDropAccumulator accumulator = new BulkDropAccumulator();
            ActiveTreeCapJobs.Job job = plugin.activeTreeCaps().begin(
                    player.getUniqueId(), accumulator, dropAt, config
            );
            List<PendingReplant> brokenLogs = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger pending = new AtomicInteger(targets.size());
            AtomicInteger breakSeq = new AtomicInteger();

            scheduleWave(
                    player,
                    tool,
                    group,
                    config,
                    dropAt,
                    targets,
                    0,
                    accumulator,
                    brokenLogs,
                    pending,
                    toolBroken,
                    breakSeq,
                    targets,
                    job
            );
        });
    }

    private void scheduleWave(
            Player player,
            ItemStack tool,
            TreeBlockGroup group,
            TreeCapitatorConfig config,
            Location dropAt,
            List<BlockPosition> positions,
            int startIndex,
            BulkDropAccumulator accumulator,
            List<PendingReplant> brokenLogs,
            AtomicInteger pending,
            AtomicBoolean toolBroken,
            AtomicInteger breakSeq,
            List<BlockPosition> chain,
            ActiveTreeCapJobs.Job job
    ) {
        if (startIndex >= positions.size()) {
            return;
        }
        if (shouldStop(job, toolBroken, player)) {
            int unscheduled = positions.size() - startIndex;
            TreeCapLog.info(config, plugin, player,
                    "async wave abort startIndex=" + startIndex
                            + " unscheduled=" + unscheduled
                            + " cancelled=" + job.isCancelled()
                            + " toolBroken=" + toolBroken.get());
            if (unscheduled > 0 && pending.addAndGet(-unscheduled) == 0) {
                finish(player, dropAt, accumulator, brokenLogs, group, config, job);
            }
            return;
        }

        int endIndex = Math.min(startIndex + config.blocksPerTick(), positions.size());
        int total = positions.size();

        Runnable wave = () -> {
            if (shouldStop(job, toolBroken, player)) {
                int unscheduled = positions.size() - startIndex;
                if (unscheduled > 0 && pending.addAndGet(-unscheduled) == 0) {
                    finish(player, dropAt, accumulator, brokenLogs, group, config, job);
                }
                return;
            }

            for (int i = startIndex; i < endIndex; i++) {
                BlockPosition position = positions.get(i);
                Location location = position.toLocation();
                plugin.getScheduler().runAtLocation(location, () -> {
                    try {
                        if (!shouldStop(job, toolBroken, player)) {
                            tryBreakBlock(
                                    player,
                                    tool,
                                    group,
                                    config,
                                    location.getBlock(),
                                    accumulator,
                                    brokenLogs,
                                    breakSeq,
                                    total,
                                    chain,
                                    toolBroken,
                                    job
                            );
                        }
                    } finally {
                        if (pending.decrementAndGet() == 0) {
                            finish(player, dropAt, accumulator, brokenLogs, group, config, job);
                        }
                    }
                });
            }

            if (endIndex < positions.size()) {
                scheduleWave(
                        player,
                        tool,
                        group,
                        config,
                        dropAt,
                        positions,
                        endIndex,
                        accumulator,
                        brokenLogs,
                        pending,
                        toolBroken,
                        breakSeq,
                        chain,
                        job
                );
            }
        };

        if (startIndex == 0) {
            wave.run();
        } else {
            plugin.getScheduler().runAtLocationLater(dropAt, wave, config.asyncDelay());
        }
    }

    private static boolean shouldStop(ActiveTreeCapJobs.Job job, AtomicBoolean toolBroken, Player player) {
        return job.isCancelled() || toolBroken.get() || !player.isOnline();
    }

    private void finish(
            Player player,
            Location dropAt,
            BulkDropAccumulator accumulator,
            List<PendingReplant> brokenLogs,
            TreeBlockGroup group,
            TreeCapitatorConfig config,
            ActiveTreeCapJobs.Job job
    ) {
        if (job.isCancelled()) {
            TreeCapLog.info(config, plugin, player, "BREAK CHAIN CANCELLED (async finish)");
            job.completeAfterCancel(plugin);
            return;
        }

        plugin.chainProgress().finish(player.getUniqueId());
        plugin.activeTreeCaps().end(job);
        List<PendingReplant> stumps = config.replant()
                ? TreeReplant.stumpsFromBrokenLogs(brokenLogs)
                : List.of();
        TreeCapLog.info(config, plugin, player,
                "BREAK CHAIN DONE " + TreeCapLog.brokenLogsSummary(brokenLogs)
                        + " " + TreeCapLog.replantSitesSummary(stumps)
                        + " " + TreeCapLog.saplingSummary(accumulator));
        Runnable spawnDrops = () -> plugin.getScheduler().runAtLocation(dropAt, () -> {
            if (config.mergeItemDrops() && !job.dropsAlreadyReleased()) {
                TreeCapLog.info(config, plugin, player,
                        "SPAWN DROPS " + TreeCapLog.formatDrops(accumulator.mergedDrops())
                                + " " + TreeCapLog.saplingSummary(accumulator));
                DropHelper.dropStacks(dropAt, accumulator.mergedDrops());
            }
            plugin.getScheduler().runOnEntity(player, () -> {
                if (player.isOnline()) {
                    notifyProcessingDone(player);
                }
            });
        });
        if (config.replant() && !stumps.isEmpty()) {
            TreeCapLog.info(config, plugin, player,
                    "REPLANT PROCESS BEGIN consume=" + config.replantConsumeSaplings()
                            + " " + TreeCapLog.replantSitesSummary(stumps)
                            + " " + TreeCapLog.saplingSummary(accumulator));
            plugin.getScheduler().runAtLocationLater(dropAt, () ->
                    TreeReplant.applyReplantsAfterBreak(
                            player,
                            stumps,
                            accumulator,
                            config.replantConsumeSaplings(),
                            config.invincibleReplant(),
                            plugin,
                            plugin.getScheduler(),
                            config,
                            spawnDrops
                    ), 1L);
        } else {
            TreeCapLog.info(config, plugin, player,
                    stumps.isEmpty()
                            ? "REPLANT PROCESS SKIPPED (no stumps from broken logs)"
                            : "REPLANT PROCESS SKIPPED (replant=false)");
            spawnDrops.run();
        }
    }

    private void tryBreakBlock(
            Player player,
            ItemStack tool,
            TreeBlockGroup group,
            TreeCapitatorConfig config,
            Block target,
            BulkDropAccumulator accumulator,
            List<PendingReplant> brokenLogs,
            AtomicInteger breakSeq,
            int total,
            List<BlockPosition> chain,
            AtomicBoolean toolBroken,
            ActiveTreeCapJobs.Job job
    ) {
        if (job.isCancelled()) {
            return;
        }
        Material targetType = target.getType();
        Location blockLoc = target.getLocation();
        int n = breakSeq.incrementAndGet();
        plugin.chainProgress().setCompleted(player.getUniqueId(), n);
        String prefix = "BLOCK " + n + "/" + total + " ";
        if (!group.matchesBlock(targetType)) {
            TreeCapLog.info(config, plugin, player,
                    prefix + "RESULT=SKIP reason=not-in-group type=" + targetType
                            + " at " + TreeCapLog.blockLabel(blockLoc, targetType)
                            + " expect=none "
                            + TreeCapLog.connectedRemaining(blockLoc, chain, group::matchesBlock));
            return;
        }
        if (!plugin.sessions().markBreaking(blockLoc)) {
            TreeCapLog.info(config, plugin, player,
                    prefix + "RESULT=SKIP reason=already-marked type=" + targetType
                            + " at " + TreeCapLog.blockLabel(blockLoc, targetType)
                            + " expect=none "
                            + TreeCapLog.connectedRemaining(blockLoc, chain, group::matchesBlock));
            return;
        }
        try {
            if (job.isCancelled()) {
                return;
            }
            BreakProtection.BreakApproval approval = BreakProtection.checkBreak(player, target);
            if (approval == null) {
                TreeCapLog.info(config, plugin, player,
                        prefix + "RESULT=SKIP reason=protected/cancelled type=" + targetType
                                + " at " + TreeCapLog.blockLabel(blockLoc, targetType)
                                + " expect=none "
                                + TreeCapLog.connectedRemaining(blockLoc, chain, group::matchesBlock));
                return;
            }
            int damageCost = config.blockDamage(targetType, player);
            if (config.damageTool() && ToolHelper.damageTool(
                    player, tool, true, config.breakTool(), damageCost
            )) {
                toolBroken.set(true);
            }
            Material below = target.getWorld().getBlockAt(
                    target.getX(), target.getY() - 1, target.getZ()).getType();
            boolean replantableLog = TreeReplant.isReplantableLogType(targetType);
            boolean plantableGround = TreeReplant.hasPlantableGround(target, targetType);
            Material expectedSapling = TreeReplant.saplingForLog(targetType);
            String expect;
            if (replantableLog) {
                brokenLogs.add(TreeReplant.recordBrokenLog(blockLoc, targetType, plantableGround));
                if (plantableGround) {
                    expect = "replantCandidate=YES replantAs=" + expectedSapling
                            + " below=" + below;
                } else {
                    expect = "replantCandidate=NO reason=bad-ground below=" + below
                            + " wouldReplantAs=" + expectedSapling;
                }
            } else {
                expect = "replantCandidate=NO reason=not-log (leaf/other)";
            }
            var drops = DropHelper.resolveDrops(target, tool, player);
            String dropText = TreeCapLog.formatDrops(drops);
            String connected = TreeCapLog.connectedRemaining(blockLoc, chain, group::matchesBlock);
            accumulator.addDrops(drops);
            if (!config.mergeItemDrops()) {
                DropHelper.spawnDrops(blockLoc, drops);
            }
            target.setType(Material.AIR);
            accumulator.incrementBlocksBroken();
            TreeCapLog.info(config, plugin, player,
                    prefix + "RESULT=BROKEN type=" + targetType
                            + " damage=" + damageCost
                            + " at " + TreeCapLog.blockLabel(blockLoc, targetType)
                            + " " + expect
                            + " " + dropText
                            + " " + connected
                            + " " + TreeCapLog.saplingSummary(accumulator)
                            + " logsBroken=" + brokenLogs.size());
        } finally {
            plugin.sessions().unmarkBreaking(blockLoc);
        }
    }

    private void notifyProcessingDone(Player player) {
        plugin.messages().send(player, "processing-done",
                java.util.Map.of("feature", "tree breaks"));
    }
}
