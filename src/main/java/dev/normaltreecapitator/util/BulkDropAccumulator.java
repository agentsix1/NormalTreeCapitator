package dev.normaltreecapitator.util;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class BulkDropAccumulator {

    private final List<ItemStack> drops = new ArrayList<>();
    private final AtomicInteger blocksBroken = new AtomicInteger();

    public synchronized void addDrops(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }
            mergeDrop(stack);
        }
    }

    public synchronized List<ItemStack> mergedDrops() {
        return drops.stream()
                .map(ItemStack::clone)
                .toList();
    }

    public void incrementBlocksBroken() {
        blocksBroken.incrementAndGet();
    }

    private void mergeDrop(ItemStack incoming) {
        ItemStack source = incoming.clone();
        for (ItemStack existing : drops) {
            if (!existing.isSimilar(source)) {
                continue;
            }
            existing.setAmount(existing.getAmount() + source.getAmount());
            return;
        }
        drops.add(source);
    }
}
