package network.paragon.hoppers.compat;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Conservative mining boundary: unavailable tool-aware loot means leave the block. */
public final class MiningCompat {
    private static final Method TOOL_DROPS = findDrops();
    private MiningCompat() {}

    private static Method findDrops() {
        try { return Block.class.getMethod("getDrops", ItemStack.class); }
        catch (NoSuchMethodException | RuntimeException | LinkageError ignored) { return null; }
    }

    /** Null means unavailable/failed, distinct from a valid empty loot result. */
    public static List<ItemStack> drops(Block block, ItemStack tool) {
        if (block == null || tool == null || TOOL_DROPS == null) return null;
        try {
            Object result = TOOL_DROPS.invoke(block, tool);
            if (!(result instanceof Collection<?>)) return null;
            List<ItemStack> copy = new ArrayList<ItemStack>();
            for (Object value : (Collection<?>) result) {
                if (!(value instanceof ItemStack)) return null;
                ItemStack item = (ItemStack) value;
                if (item.getAmount() > 0 && !isAir(item.getType())) copy.add(item.clone());
            }
            return copy;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static boolean isAir(Material material) {
        if (material == null) return false;
        String name = material.name();
        return "AIR".equals(name) || "CAVE_AIR".equals(name) || "VOID_AIR".equals(name);
    }

    /** Legacy lit redstone is the same quarry target as unlit redstone, not a new ore. */
    public static boolean matches(java.util.Set<Material> configured, Material block) {
        return configured.contains(block) || (block != null && "GLOWING_REDSTONE_ORE".equals(block.name())
                && configured.contains(MaterialCompat.resolve("REDSTONE_ORE")));
    }

    /** Recheck protections at the mutation boundary, even when called outside MiningEngine. */
    public static boolean remove(Block block, Material expected) {
        if (block == null || expected == null || isAir(expected)) return false;
        Material current = block.getType();
        if (current != expected || MaterialCompat.isBedrock(current)
                || MaterialCompat.isProtectedContainer(current)
                || block.getState() instanceof InventoryHolder) return false;
        Material air = MaterialCompat.resolve("AIR");
        if (air == null) return false;
        try {
            // Oldest stable Bukkit overload; retain normal physics and never spawn loot twice.
            block.setType(air);
        } catch (RuntimeException | LinkageError failure) {
            // Some implementations mutate before throwing; inspect the result before rollback.
            return isAir(block.getType());
        }
        return isAir(block.getType());
    }
}
