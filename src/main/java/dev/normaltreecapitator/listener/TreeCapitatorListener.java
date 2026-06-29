package dev.normaltreecapitator.listener;

import dev.normaltreecapitator.NormalTreeCapitator;
import dev.normaltreecapitator.config.TreeBlockGroup;
import dev.normaltreecapitator.config.TreeCapitatorConfig;
import dev.normaltreecapitator.playerdata.PlayerData;
import dev.normaltreecapitator.util.AdjacentFlooder;
import dev.normaltreecapitator.util.BlockPosition;
import dev.normaltreecapitator.util.BulkBreakExecutor;
import dev.normaltreecapitator.util.DropHelper;
import dev.normaltreecapitator.util.ReplantHelper;
import dev.normaltreecapitator.util.ToolHelper;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TreeCapitatorListener implements Listener {

    private final NormalTreeCapitator plugin;
    private final BulkBreakExecutor asyncExecutor;

    public TreeCapitatorListener(NormalTreeCapitator plugin) {
        this.plugin = plugin;
        this.asyncExecutor = new BulkBreakExecutor(plugin);
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
        TreeBlockGroup group = config.groupFor(material);
        if (group == null) {
            return;
        }

        if (!isFeatureEnabled(player, config)) {
            return;
        }
        if (config.mustSneak() && !player.isSneaking()) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!toolAllowed(tool, config)) {
            return;
        }

        int limit = chainLimit(group);
        List<BlockPosition> targets = collectBlocks(block, group, config, limit);
        if (targets.size() <= 1) {
            return;
        }

        event.setCancelled(true);

        if (targets.size() > config.asyncStart()) {
            asyncExecutor.execute(player, tool, group, config, block.getLocation(), targets);
            return;
        }

        AtomicBoolean toolBroken = new AtomicBoolean(false);
        for (BlockPosition pos : targets) {
            Location loc = pos.toLocation();
            scheduleBreak(player, tool, group, config, loc, toolBroken);
        }
    }

    private void scheduleBreak(
            Player player,
            ItemStack tool,
            TreeBlockGroup group,
            TreeCapitatorConfig config,
            Location loc,
            AtomicBoolean toolBroken
    ) {
        plugin.getScheduler().runOnEntity(player, () -> {
            if (toolBroken.get() || !player.isOnline()) {
                return;
            }
            if (config.damageTool() && ToolHelper.damageTool(
                    player, tool, config.needTool(), config.breakTool()
            )) {
                toolBroken.set(true);
                return;
            }
            plugin.getScheduler().runAtLocation(loc, () -> breakBlock(
                    player, tool, group, config, loc, toolBroken
            ));
        });
    }

    private boolean isFeatureEnabled(Player player, TreeCapitatorConfig config) {
        if (!player.hasPermission("normaltreecapitator.use")) {
            return false;
        }
        PlayerData data = plugin.playerData().get(player.getUniqueId(), config);
        return data.enabled();
    }

    private boolean toolAllowed(ItemStack tool, TreeCapitatorConfig config) {
        if (!config.allowsTreeTool(tool.getType())) {
            return false;
        }
        if (!config.needTool()) {
            return true;
        }
        return ToolHelper.axeUsable(tool, true, config.breakTool());
    }

    private int chainLimit(TreeBlockGroup group) {
        int limit = group.maxChain();
        return limit < 0 ? Integer.MAX_VALUE : limit;
    }

    private List<BlockPosition> collectBlocks(
            Block origin,
            TreeBlockGroup group,
            TreeCapitatorConfig config,
            int limit
    ) {
        BlockPosition start = BlockPosition.of(origin.getLocation());
        return AdjacentFlooder.floodFill(start, config::isTreeBlock, limit, group.searchRadius());
    }

    private void breakBlock(
            Player player,
            ItemStack tool,
            TreeBlockGroup group,
            TreeCapitatorConfig config,
            Location loc,
            AtomicBoolean toolBroken
    ) {
        if (toolBroken.get()) {
            return;
        }
        Block target = loc.getBlock();
        Material targetType = target.getType();
        if (!config.isTreeBlock(targetType)) {
            return;
        }
        if (!plugin.sessions().markBreaking(loc)) {
            return;
        }
        try {
            if (config.replant() && group.isReplantableLog(targetType)
                    && ReplantHelper.replant(target, targetType, config.invincibleReplant(), plugin)) {
                return;
            }
            Location at = loc.clone().add(0.5, 0.5, 0.5);
            for (ItemStack drop : DropHelper.resolveDrops(target, tool, player)) {
                target.getWorld().dropItemNaturally(at, drop);
            }
            target.setType(Material.AIR);
        } finally {
            plugin.sessions().unmarkBreaking(loc);
        }
    }

    private boolean handleInvincibleReplant(BlockBreakEvent event, Player player, Block block) {
        if (!block.hasMetadata(ReplantHelper.META_INV_REPL)) {
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
        block.removeMetadata(ReplantHelper.META_INV_REPL, plugin);
        Block below = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());
        below.removeMetadata(ReplantHelper.META_INV_REPL, plugin);
        Block above = block.getWorld().getBlockAt(block.getX(), block.getY() + 1, block.getZ());
        above.removeMetadata(ReplantHelper.META_INV_REPL, plugin);
    }
}
