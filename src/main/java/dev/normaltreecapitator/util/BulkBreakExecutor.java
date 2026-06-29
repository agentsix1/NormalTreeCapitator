package dev.normaltreecapitator.util;

import dev.normaltreecapitator.NormalTreeCapitator;
import dev.normaltreecapitator.config.TreeBlockGroup;
import dev.normaltreecapitator.config.TreeCapitatorConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Breaks large tree chains in timed waves, merging all drops at the origin block.
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
                return;
            }

            AtomicBoolean toolBroken = new AtomicBoolean(false);
            List<BlockPosition> toBreak = applyUpfrontDurability(player, tool, config, targets, toolBroken);
            if (toBreak.isEmpty()) {
                return;
            }

            BulkDropAccumulator accumulator = new BulkDropAccumulator();
            AtomicInteger pending = new AtomicInteger(toBreak.size());

            scheduleWave(
                    player,
                    tool,
                    group,
                    config,
                    dropAt,
                    toBreak,
                    0,
                    accumulator,
                    pending,
                    toolBroken
            );
        });
    }

    private List<BlockPosition> applyUpfrontDurability(
            Player player,
            ItemStack tool,
            TreeCapitatorConfig config,
            List<BlockPosition> targets,
            AtomicBoolean toolBroken
    ) {
        if (!config.damageTool() || ToolHelper.isUnbreakable(tool)) {
            return targets;
        }

        int allowed = 0;
        for (int i = 0; i < targets.size(); i++) {
            if (ToolHelper.damageTool(player, tool, config.needTool(), config.breakTool())) {
                toolBroken.set(true);
                break;
            }
            allowed++;
        }
        if (allowed <= 0) {
            return List.of();
        }
        if (allowed == targets.size()) {
            return targets;
        }
        return List.copyOf(targets.subList(0, allowed));
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
            AtomicInteger pending,
            AtomicBoolean toolBroken
    ) {
        if (startIndex >= positions.size()) {
            return;
        }
        if (toolBroken.get() || !player.isOnline()) {
            int unscheduled = positions.size() - startIndex;
            if (unscheduled > 0 && pending.addAndGet(-unscheduled) == 0) {
                finish(dropAt, accumulator);
            }
            return;
        }

        int endIndex = Math.min(startIndex + config.blocksPerTick(), positions.size());

        Runnable wave = () -> {
            if (toolBroken.get() || !player.isOnline()) {
                int unscheduled = positions.size() - startIndex;
                if (unscheduled > 0 && pending.addAndGet(-unscheduled) == 0) {
                    finish(dropAt, accumulator);
                }
                return;
            }

            for (int i = startIndex; i < endIndex; i++) {
                BlockPosition position = positions.get(i);
                Location location = position.toLocation();
                plugin.getScheduler().runAtLocation(location, () -> {
                    try {
                        if (!toolBroken.get() && player.isOnline()) {
                            tryBreakBlock(player, tool, group, config, location.getBlock(), accumulator);
                        }
                    } finally {
                        if (pending.decrementAndGet() == 0) {
                            finish(dropAt, accumulator);
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
                        pending,
                        toolBroken
                );
            }
        };

        if (startIndex == 0) {
            wave.run();
        } else {
            plugin.getScheduler().runAtLocationLater(dropAt, wave, 1L);
        }
    }

    private void finish(Location dropAt, BulkDropAccumulator accumulator) {
        plugin.getScheduler().runAtLocation(dropAt, () ->
                DropHelper.dropStacks(dropAt, accumulator.mergedDrops()));
    }

    private void tryBreakBlock(
            Player player,
            ItemStack tool,
            TreeBlockGroup group,
            TreeCapitatorConfig config,
            Block target,
            BulkDropAccumulator accumulator
    ) {
        Material targetType = target.getType();
        if (!config.isTreeBlock(targetType)) {
            return;
        }
        if (!plugin.sessions().markBreaking(target.getLocation())) {
            return;
        }
        try {
            if (config.replant() && group.isReplantableLog(targetType)
                    && ReplantHelper.replant(target, targetType, config.invincibleReplant(), plugin)) {
                accumulator.incrementBlocksBroken();
                return;
            }
            accumulator.addDrops(DropHelper.resolveDrops(target, tool, player));
            target.setType(Material.AIR);
            accumulator.incrementBlocksBroken();
        } finally {
            plugin.sessions().unmarkBreaking(target.getLocation());
        }
    }
}
