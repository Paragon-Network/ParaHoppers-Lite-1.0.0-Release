package network.paragon.hoppers.compat;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Compatibility boundary for Bukkit inventory operations used by ParaHoppers.
 *
 * Keep GUI/container logic here so shared features do not need to depend on
 * newer container implementations or newer click-view helpers.
 */
public final class InventoryCompat {
    private InventoryCompat() {}

    public static Inventory create(InventoryHolder holder, int requestedSize, String title) {
        int size = normalizeChestSize(requestedSize);
        String safeTitle = title == null ? "Para Hopper" : title;
        // Legacy clients/servers have much smaller inventory-title limits.
        // 32 chars is safe across the old Bukkit generations targeted here.
        if (safeTitle.length() > 32) safeTitle = safeTitle.substring(0, 32);
        return Bukkit.createInventory(holder, size, safeTitle);
    }

    public static int normalizeChestSize(int requestedSize) {
        int size = Math.max(9, Math.min(54, requestedSize));
        int remainder = size % 9;
        if (remainder != 0) size += 9 - remainder;
        return Math.min(54, size);
    }

    public static InventoryHolder holder(InventoryClickEvent event) {
        Inventory top = event.getInventory();
        return top == null ? null : top.getHolder();
    }

    public static InventoryHolder holder(InventoryDragEvent event) {
        Inventory top = event.getInventory();
        return top == null ? null : top.getHolder();
    }

    /** True when the raw slot belongs to the plugin's top inventory. */
    public static boolean isTopClick(InventoryClickEvent event) {
        Inventory top = event.getInventory();
        int raw = event.getRawSlot();
        return top != null && raw >= 0 && raw < top.getSize();
    }

    /**
     * Legacy-safe replacement for InventoryClickEvent#getClickedInventory().
     * Raw slots after the top inventory belong to the player's inventory.
     */
    public static boolean isPlayerInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getInventory();
        int raw = event.getRawSlot();
        return top != null && raw >= top.getSize();
    }

    /** Returns an inventory from any Bukkit block state that exposes one. */
    public static Inventory inventory(BlockState state) {
        if (!(state instanceof InventoryHolder)) return null;
        return ((InventoryHolder) state).getInventory();
    }

    public static int addAndCount(Inventory destination, ItemStack stack) {
        if (destination == null || stack == null || stack.getAmount() <= 0) return 0;
        int requested = stack.getAmount();
        Map<Integer, ItemStack> leftovers = destination.addItem(stack.clone());
        int remaining = 0;
        for (ItemStack item : leftovers.values()) {
            if (item != null) remaining += item.getAmount();
        }
        return Math.max(0, requested - remaining);
    }

    /**
     * Moves at most maxAmount from a source slot. The source is only reduced by
     * the amount that actually fitted in the destination inventory.
     */
    public static int transferSlot(Inventory source, int slot, Inventory destination, int maxAmount) {
        if (source == null || destination == null || maxAmount <= 0) return 0;
        ItemStack stack = source.getItem(slot);
        if (stack == null || stack.getAmount() <= 0) return 0;

        int moving = Math.min(maxAmount, stack.getAmount());
        ItemStack part = stack.clone();
        part.setAmount(moving);
        int moved = addAndCount(destination, part);
        if (moved <= 0) return 0;

        int left = stack.getAmount() - moved;
        if (left <= 0) source.setItem(slot, null);
        else {
            stack.setAmount(left);
            source.setItem(slot, stack);
        }
        return moved;
    }
}
