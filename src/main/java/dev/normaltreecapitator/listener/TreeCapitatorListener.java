package dev.normaltreecapitator.listener;

import dev.normaltreecapitator.NormalTreeCapitator;
import dev.normaltreecapitator.config.TreeBlockGroup;
import dev.normaltreecapitator.config.TreeCapitatorConfig;
import dev.normaltreecapitator.playerdata.PlayerData;
import dev.normaltreecapitator.util.AdjacentFlooder;
import dev.normaltreecapitator.util.BlockPosition;
import dev.normaltreecapitator.util.BreakProtection;
import dev.normaltreecapitator.util.BulkBreakExecutor;
import dev.normaltreecapitator.util.BulkDropAccumulator;
import dev.normaltreecapitator.util.ChainLimiter;
import dev.normaltreecapitator.util.DropHelper;
import dev.normaltreecapitator.util.PendingReplant;
import dev.normaltreecapitator.util.ToolHelper;
import dev.normaltreecapitator.util.TreeCapLog;
import dev.normaltreecapitator.util.TreeCapSneak;
import dev.normaltreecapitator.util.TreeReplant;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class TreeCapitatorListener implements Listener {

    private final NormalTreeCapitator plugin;
    private final BulkBreakExecutor asyncExecutor;

    public TreeCapitatorListener(NormalTreeCapitator plugin) {
        this.plugin = plugin;
        this.asyncExecutor = new BulkBreakExecutor(plugin);
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        plugin.sessions().setShiftHeld(event.getPlayer().getUniqueId(), event.isSneaking());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.sessions().setShiftHeld(event.getPlayer().getUniqueId(), event.getPlayer().isSneaking());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.sessions().setShiftHeld(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.sessions().isPluginBreak(event.getBlock().getLocation())) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        TreeCapitatorConfig config = plugin.config();

        if (config.invincibleReplant() && handleInvincibleReplant(event, player, block)) {
            return;
        }

        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        Material material = block.getType();
        ItemStack tool = player.getInventory().getItemInMainHand();
        TreeBlockGroup group = config.groupFor(material, tool.getType(), player);
        if (group == null) {
            return;
        }

        if (!isFeatureEnabled(player, config)) {
            return;
        }

        handleTreeCapitator(event, player, block, material, tool, group, config);
    }

    private void handleTreeCapitator(
            BlockBreakEvent event,
            Player player,
            Block block,
            Material material,
            ItemStack tool,
            TreeBlockGroup group,
            TreeCapitatorConfig config
    ) {
        boolean sneaking = resolveSneaking(player);

        TreeCapLog.info(config, plugin, player,
                "evaluate sneaking=" + sneaking
                        + " isSneaking=" + player.isSneaking()
                        + " shiftHeld=" + plugin.sessions().isShiftHeld(player.getUniqueId())
                        + " mustSneak=" + config.mustSneak()
                        + " origin=" + TreeCapLog.blockLabel(block.getLocation(), material));

        if (!TreeCapSneak.allowsActivation(config, sneaking)) {
            if (config.mustSneak() && !sneaking) {
                TreeCapLog.info(config, plugin, player,
                        "blocked: must-sneak=true but player is not sneaking"
                                + " origin=" + TreeCapLog.blockLabel(block.getLocation(), material));
            } else {
                TreeCapLog.info(config, plugin, player,
                        "blocked: must-sneak=false requires standing (not sneaking)"
                                + " origin=" + TreeCapLog.blockLabel(block.getLocation(), material));
            }
            return;
        }
        TreeCapLog.info(config, plugin, player,
                "sneak gate passed sneaking=" + sneaking
                        + " mustSneak=" + config.mustSneak());
        if (plugin.sessions().onCooldown(player.getUniqueId())) {
            TreeCapLog.info(config, plugin, player,
                    "blocked: tree capitator on cooldown"
                            + " origin=" + TreeCapLog.blockLabel(block.getLocation(), material));
            return;
        }
        if (!toolAllowed(tool, group, config)) {
            TreeCapLog.info(config, plugin, player,
                    "blocked: tool not allowed for treecap tool=" + tool.getType()
                            + " group=" + group.id());
            return;
        }

        int limit = chainLimit(group);
        List<BlockPosition> collected = collectBlocks(block, group, limit);
        List<BlockPosition> targets = ChainLimiter.limitToToolBudget(collected, tool, config, player);
        if (targets.isEmpty()) {
            if (collected.isEmpty()) {
                TreeCapLog.info(config, plugin, player,
                        "skip: no connected blocks"
                                + " origin=" + TreeCapLog.blockLabel(block.getLocation(), material)
                                + " groupBlocks=" + group.blocks().size()
                                + " originInGroup=" + group.matchesBlock(material));
            } else {
                TreeCapLog.info(config, plugin, player,
                        "skip: tool durability budget exhausted"
                                + " origin=" + TreeCapLog.blockLabel(block.getLocation(), material)
                                + " chainSize=" + collected.size()
                                + " tool=" + tool.getType());
            }
            return;
        }

        event.setCancelled(true);
        applyCooldown(player, config.cooldownTicks());
        notifyTreeProcessing(player);

        List<PendingReplant> replantPreview = config.replant()
                ? TreeReplant.findStumpSites(targets)
                : List.of();
        boolean async = targets.size() > config.asyncStart();
        int trunks = 0;
        for (BlockPosition pos : targets) {
            Material type = AdjacentFlooder.safeType(pos.world(), pos.x(), pos.y(), pos.z());
            if (type != null && AdjacentFlooder.isTrunkMaterial(type)) {
                trunks++;
            }
        }
        TreeCapLog.info(config, plugin, player,
                "START origin=" + TreeCapLog.blockLabel(block.getLocation(), material)
                        + " group=" + group.id()
                        + " groupBlocks=" + group.blocks().size()
                        + " originInGroup=" + group.matchesBlock(material)
                        + " maxChain=" + limit
                        + " searchRadius=" + group.searchRadius()
                        + " trunksInChain=" + trunks
                        + " tool=" + tool.getType()
                        + " mode=" + (async ? "async" : "sync")
                        + " sneaking=" + sneaking
                        + " mustSneak=" + config.mustSneak()
                        + " replant=" + config.replant()
                        + " consumeSaplings=" + config.replantConsumeSaplings()
                        + " mergeDrops=" + config.mergeItemDrops()
                        + " cooldownTicks=" + config.cooldownTicks()
                        + " " + TreeCapLog.chainSummary(targets)
                        + " " + TreeCapLog.replantSitesSummary(replantPreview));
        if (async) {
            asyncExecutor.execute(player, tool, group, config, block.getLocation(), targets);
        } else {
            executeSyncTreeCap(player, tool, group, config, block.getLocation(), targets);
        }
    }

    private void executeSyncTreeCap(
            Player player,
            ItemStack tool,
            TreeBlockGroup group,
            TreeCapitatorConfig config,
            Location origin,
            List<BlockPosition> targets
    ) {
        TreeCapLog.info(config, plugin, player,
                "sync chain begin targets=" + targets.size()
                        + " origin=" + TreeCapLog.blockLabel(origin, origin.getBlock().getType()));
        Location dropAt = origin.clone().add(0.5, 0.5, 0.5);
        BulkDropAccumulator accumulator = new BulkDropAccumulator();
        List<PendingReplant> brokenLogs = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger pending = new AtomicInteger(targets.size());
        AtomicInteger breakSeq = new AtomicInteger();
        AtomicBoolean toolBroken = new AtomicBoolean(false);
        int total = targets.size();

        for (BlockPosition pos : targets) {
            Location loc = pos.toLocation();
            plugin.getScheduler().runOnEntity(player, () -> {
                if (toolBroken.get() || !player.isOnline()) {
                    TreeCapLog.info(config, plugin, player,
                            "break task aborted online=" + player.isOnline()
                                    + " toolBroken=" + toolBroken.get()
                                    + " at " + TreeCapLog.blockLabel(loc, loc.getBlock().getType()));
                    if (pending.decrementAndGet() == 0) {
                        finishSyncTreeCap(dropAt, accumulator, brokenLogs, config, player);
                    }
                    return;
                }
                plugin.getScheduler().runAtLocation(loc, () -> {
                    try {
                        if (!toolBroken.get() && player.isOnline()) {
                            breakTreeBlockForDeferredReplant(
                                    player, tool, group, config, loc, accumulator, brokenLogs,
                                    breakSeq, total, targets, toolBroken
                            );
                        } else {
                            TreeCapLog.info(config, plugin, player,
                                    "break skipped toolBroken=" + toolBroken.get()
                                            + " at " + TreeCapLog.blockLabel(loc, loc.getBlock().getType()));
                        }
                    } finally {
                        if (pending.decrementAndGet() == 0) {
                            finishSyncTreeCap(dropAt, accumulator, brokenLogs, config, player);
                        }
                    }
                });
            });
        }
    }

    private void breakTreeBlockForDeferredReplant(
            Player player,
            ItemStack tool,
            TreeBlockGroup group,
            TreeCapitatorConfig config,
            Location loc,
            BulkDropAccumulator accumulator,
            List<PendingReplant> brokenLogs,
            AtomicInteger breakSeq,
            int total,
            List<BlockPosition> chain,
            AtomicBoolean toolBroken
    ) {
        Block target = loc.getBlock();
        Material targetType = target.getType();
        int n = breakSeq.incrementAndGet();
        String prefix = "BLOCK " + n + "/" + total + " ";

        if (!group.matchesBlock(targetType)) {
            TreeCapLog.info(config, plugin, player,
                    prefix + "RESULT=SKIP reason=not-in-group type=" + targetType
                            + " at " + TreeCapLog.blockLabel(loc, targetType)
                            + " expect=none "
                            + TreeCapLog.connectedRemaining(loc, chain, group::matchesBlock));
            return;
        }
        if (!plugin.sessions().markBreaking(loc)) {
            TreeCapLog.info(config, plugin, player,
                    prefix + "RESULT=SKIP reason=already-marked type=" + targetType
                            + " at " + TreeCapLog.blockLabel(loc, targetType)
                            + " expect=none "
                            + TreeCapLog.connectedRemaining(loc, chain, group::matchesBlock));
            return;
        }
        try {
            BreakProtection.BreakApproval approval = BreakProtection.checkBreak(player, target);
            if (approval == null) {
                TreeCapLog.info(config, plugin, player,
                        prefix + "RESULT=SKIP reason=protected/cancelled type=" + targetType
                                + " at " + TreeCapLog.blockLabel(loc, targetType)
                                + " expect=none "
                                + TreeCapLog.connectedRemaining(loc, chain, group::matchesBlock));
                return;
            }

            int damageCost = config.blockDamage(targetType, player);
            if (config.damageTool() && ToolHelper.damageTool(
                    player, tool, true, config.breakTool(), damageCost
            )) {
                toolBroken.set(true);
                TreeCapLog.info(config, plugin, player,
                        prefix + "tool exhausted after damage=" + damageCost);
            }

            Material below = target.getWorld().getBlockAt(
                    target.getX(), target.getY() - 1, target.getZ()).getType();
            boolean replantableLog = TreeReplant.isReplantableLogType(targetType);
            boolean plantableGround = TreeReplant.hasPlantableGround(target, targetType);
            Material expectedSapling = TreeReplant.saplingForLog(targetType);

            String expect;
            if (replantableLog) {
                brokenLogs.add(TreeReplant.recordBrokenLog(loc, targetType, plantableGround));
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
            String connected = TreeCapLog.connectedRemaining(loc, chain, group::matchesBlock);
            accumulator.addDrops(drops);
            if (!config.mergeItemDrops()) {
                DropHelper.spawnDrops(loc, drops);
            }
            target.setType(Material.AIR);

            TreeCapLog.info(config, plugin, player,
                    prefix + "RESULT=BROKEN type=" + targetType
                            + " damage=" + damageCost
                            + " at " + TreeCapLog.blockLabel(loc, targetType)
                            + " " + expect
                            + " " + dropText
                            + " " + connected
                            + " " + TreeCapLog.saplingSummary(accumulator)
                            + " logsBroken=" + brokenLogs.size());
        } finally {
            plugin.sessions().unmarkBreaking(loc);
        }
    }

    private void finishSyncTreeCap(
            Location dropAt,
            BulkDropAccumulator accumulator,
            List<PendingReplant> brokenLogs,
            TreeCapitatorConfig config,
            Player player
    ) {
        List<PendingReplant> stumps = config.replant()
                ? TreeReplant.stumpsFromBrokenLogs(brokenLogs)
                : List.of();
        TreeCapLog.info(config, plugin, player,
                "BREAK CHAIN DONE " + TreeCapLog.brokenLogsSummary(brokenLogs)
                        + " " + TreeCapLog.replantSitesSummary(stumps)
                        + " " + TreeCapLog.saplingSummary(accumulator));
        Runnable spawnDrops = () -> plugin.getScheduler().runAtLocation(dropAt, () -> {
            if (config.mergeItemDrops()) {
                TreeCapLog.info(config, plugin, player,
                        "SPAWN DROPS " + TreeCapLog.formatDrops(accumulator.mergedDrops())
                                + " " + TreeCapLog.saplingSummary(accumulator));
                DropHelper.dropStacks(dropAt, accumulator.mergedDrops());
            }
            notifyTreeProcessingDone(player);
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

    private boolean resolveSneaking(Player player) {
        return player.isSneaking() || plugin.sessions().isShiftHeld(player.getUniqueId());
    }

    private boolean isFeatureEnabled(Player player, TreeCapitatorConfig config) {
        if (!player.hasPermission("normaltreecapitator.use")) {
            TreeCapLog.info(config, plugin, player,
                    "blocked: missing permission normaltreecapitator.use");
            return false;
        }
        PlayerData data = plugin.playerData().get(player.getUniqueId(), config);
        if (!data.enabled()) {
            TreeCapLog.info(config, plugin, player,
                    "blocked: tree capitator toggled off (/tc toggle)");
            return false;
        }
        return true;
    }

    private void notifyTreeProcessing(Player player) {
        plugin.messages().send(player, "processing",
                java.util.Map.of("feature", "tree breaks"));
    }

    private void notifyTreeProcessingDone(Player player) {
        plugin.getScheduler().runOnEntity(player, () -> {
            if (player.isOnline()) {
                plugin.messages().send(player, "processing-done",
                        java.util.Map.of("feature", "tree breaks"));
            }
        });
    }

    private boolean toolAllowed(ItemStack tool, TreeBlockGroup group, TreeCapitatorConfig config) {
        if (!config.needTool()) {
            return true;
        }
        // Group membership for this tool was already required in groupFor(...).
        if (!group.allowsTool(tool.getType())) {
            return false;
        }
        return ToolHelper.axeUsable(tool, true, config.breakTool());
    }

    private int chainLimit(TreeBlockGroup group) {
        int limit = group.maxChain();
        return limit < 0 ? Integer.MAX_VALUE : limit;
    }

    private List<BlockPosition> collectBlocks(Block origin, TreeBlockGroup group, int limit) {
        Set<Material> allowed = group.blocks();
        List<BlockPosition> found = AdjacentFlooder.floodFill(
                BlockPosition.of(origin.getLocation()),
                allowed::contains,
                limit,
                group.searchRadius()
        );
        return prioritizeTrunks(found);
    }

    private static List<BlockPosition> prioritizeTrunks(List<BlockPosition> found) {
        if (found == null || found.size() <= 1) {
            return found;
        }
        List<BlockPosition> trunks = new ArrayList<>(found.size());
        List<BlockPosition> other = new ArrayList<>(found.size());
        for (BlockPosition pos : found) {
            Material type = AdjacentFlooder.safeType(pos.world(), pos.x(), pos.y(), pos.z());
            if (type != null && AdjacentFlooder.isTrunkMaterial(type)) {
                trunks.add(pos);
            } else {
                other.add(pos);
            }
        }
        if (trunks.isEmpty()) {
            return found;
        }
        List<BlockPosition> ordered = new ArrayList<>(found.size());
        ordered.addAll(trunks);
        ordered.addAll(other);
        return ordered;
    }

    private void applyCooldown(Player player, int ticks) {
        if (ticks <= 0) {
            return;
        }
        var uuid = player.getUniqueId();
        plugin.sessions().addCooldown(uuid);
        plugin.getScheduler().runOnEntityLater(
                player,
                () -> plugin.sessions().endCooldown(uuid),
                ticks
        );
    }

    private boolean handleInvincibleReplant(BlockBreakEvent event, Player player, Block block) {
        if (!block.hasMetadata(TreeReplant.META_INV_REPL)) {
            return false;
        }
        if (player.hasPermission("normaltreecapitator.admin")) {
            clearReplantMeta(block);
            return false;
        }
        event.setCancelled(true);
        plugin.messages().send(player, "sapling-protected");
        return true;
    }

    private void clearReplantMeta(Block block) {
        block.removeMetadata(TreeReplant.META_INV_REPL, plugin);
        Block below = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());
        below.removeMetadata(TreeReplant.META_INV_REPL, plugin);
        Block above = block.getWorld().getBlockAt(block.getX(), block.getY() + 1, block.getZ());
        above.removeMetadata(TreeReplant.META_INV_REPL, plugin);
    }
}
