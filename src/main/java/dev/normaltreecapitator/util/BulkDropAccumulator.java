package dev.normaltreecapitator.util;

import org.bukkit.Material;
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

    /** Returns a snapshot of merged drops and clears the accumulator (for cancel flush). */
    public synchronized List<ItemStack> takeMergedDrops() {
        List<ItemStack> out = drops.stream()
                .map(ItemStack::clone)
                .toList();
        drops.clear();
        return out;
    }

    public synchronized boolean tryConsumeSapling(Material sapling) {
        for (int i = 0; i < drops.size(); i++) {
            ItemStack stack = drops.get(i);
            if (stack.getType() != sapling || stack.getAmount() <= 0) {
                continue;
            }
            if (stack.getAmount() == 1) {
                drops.remove(i);
            } else {
                stack.setAmount(stack.getAmount() - 1);
            }
            return true;
        }
        return false;
    }

    public synchronized int countOf(Material material) {
        int total = 0;
        for (ItemStack stack : drops) {
            if (stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public synchronized String debugSaplingCounts() {
        StringBuilder out = new StringBuilder("drops=");
        if (drops.isEmpty()) {
            return out.append("[]").toString();
        }
        out.append('[');
        boolean first = true;
        for (ItemStack stack : drops) {
            Material type = stack.getType();
            String name = type.name();
            if (!name.contains("SAPLING")
                    && !name.contains("PROPAGULE")
                    && !name.contains("FUNGUS")
                    && !name.contains("LOG")
                    && !name.contains("LEAVES")
                    && !name.contains("STEM")
                    && !name.contains("WART")
                    && !name.contains("WOOD")) {
                continue;
            }
            if (!first) {
                out.append(", ");
            }
            out.append(type).append('x').append(stack.getAmount());
            first = false;
        }
        if (first) {
            out.append("no-tree-items, totalStacks=").append(drops.size());
        }
        return out.append(']').toString();
    }

    public void incrementBlocksBroken() {
        blocksBroken.incrementAndGet();
    }

    public int blocksBroken() {
        return blocksBroken.get();
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
