package network.paragon.hoppers.service;

import network.paragon.hoppers.compat.MiningCompat;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.Collection;
import java.util.function.BooleanSupplier;

/** Single-server-thread transaction. Prepare exact stacks before changing the world. */
public final class MiningTransfer {
    private MiningTransfer() {}

    /** Null means all drops cannot fit. Neither original stacks nor loot are mutated. */
    public static ItemStack[] plan(ItemStack[] original, int inventoryLimit, Collection<ItemStack> drops) {
        ItemStack[] result = copy(original);
        for (ItemStack drop : drops) {
            int remaining = drop.getAmount();
            if (remaining <= 0 || MiningCompat.isAir(drop.getType())) continue;
            int limit = Math.min(inventoryLimit, drop.getMaxStackSize());
            if (limit < 1) return null;
            // Fill existing compatible stacks before allocating empty slots.
            for (int i = 0; i < result.length && remaining > 0; i++) {
                ItemStack slot = result[i];
                if (slot == null || !slot.isSimilar(drop)) continue;
                int moved = Math.min(remaining, Math.max(0, limit - slot.getAmount()));
                slot.setAmount(slot.getAmount() + moved);
                remaining -= moved;
            }
            for (int i = 0; i < result.length && remaining > 0; i++) {
                ItemStack slot = result[i];
                if (slot != null && slot.getAmount() > 0 && !MiningCompat.isAir(slot.getType())) continue;
                int moved = Math.min(remaining, limit);
                result[i] = drop.clone();
                result[i].setAmount(moved);
                remaining -= moved;
            }
            if (remaining > 0) return null;
        }
        return result;
    }

    public static boolean commit(Inventory inventory, ItemStack[] before, ItemStack[] after,
                                 BooleanSupplier removeBlock) {
        try {
            inventory.setContents(copy(after));
        } catch (RuntimeException | LinkageError failure) {
            inventory.setContents(copy(before));
            throw failure;
        }
        boolean removed;
        try {
            removed = removeBlock.getAsBoolean();
        } catch (RuntimeException | LinkageError failure) {
            // Unknown world state: stop mining instead of guessing and minting drops on retry.
            throw new IllegalStateException("Block removal outcome is unknown; inspect hopper and block before restarting mining", failure);
        }
        if (!removed) inventory.setContents(copy(before));
        return removed;
    }

    public static ItemStack[] copy(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) copy[i] = source[i] == null ? null : source[i].clone();
        return copy;
    }
}
