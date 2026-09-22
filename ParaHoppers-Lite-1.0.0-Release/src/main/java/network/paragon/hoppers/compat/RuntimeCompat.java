package network.paragon.hoppers.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Bridges changing descriptors as well as missing methods. No NMS or Paper linkage. */
public final class RuntimeCompat {
    private static final Method HAND = method(PlayerInteractEvent.class, "getHand");
    private static final Method ONLINE = method(Bukkit.class, "getOnlinePlayers");
    private static final Method EXACT_TARGET = method(Player.class, "getTargetBlockExact", int.class);
    private static final Method OLD_TARGET = oldTarget();
    private static final Method NEARBY = method(World.class, "getNearbyEntities", Location.class, double.class, double.class, double.class);
    private static final Method SUPPRESS_DROPS = method(BlockBreakEvent.class, "setDropItems", boolean.class);
    private RuntimeCompat() {}

    private static Method method(Class<?> owner, String name, Class<?>... parameters) {
        try { return owner.getMethod(name, parameters); }
        catch (NoSuchMethodException | RuntimeException | LinkageError unavailable) { return null; }
    }

    public static boolean mainHand(PlayerInteractEvent event) {
        if (HAND == null) return true; // Pre-dual-wield event.
        try { return "HAND".equals(String.valueOf(HAND.invoke(event))); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
    }

    /** Bukkit changed the return descriptor from Player[] to Collection. */
    public static List<Player> onlinePlayers() {
        List<Player> result = new ArrayList<Player>();
        if (ONLINE == null) return result;
        try {
            Object value = ONLINE.invoke(null);
            if (value instanceof Iterable<?>) for (Object player : (Iterable<?>) value) {
                if (player instanceof Player) result.add((Player) player);
            }
            else if (value instanceof Object[]) for (Object player : (Object[]) value) {
                if (player instanceof Player) result.add((Player) player);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { }
        return result;
    }

    private static Method oldTarget() {
        // HashSet<Byte> on 1.7; Set<Material> on later Bukkit. null means default transparency.
        try {
            for (Method candidate : Player.class.getMethods()) {
                Class<?>[] args = candidate.getParameterTypes();
                if (candidate.getName().equals("getTargetBlock") && args.length == 2
                        && Set.class.isAssignableFrom(args[0]) && args[1] == int.class) return candidate;
            }
        } catch (RuntimeException | LinkageError unavailable) { }
        return null;
    }

    public static Block targetBlock(Player player, int distance) {
        if (player == null) return null;
        try {
            if (EXACT_TARGET != null) return (Block) EXACT_TARGET.invoke(player, distance);
            if (OLD_TARGET != null) return (Block) OLD_TARGET.invoke(player, null, distance);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { }
        return null;
    }

    public static Collection<Item> nearbyItems(World world, Location center, double radius) {
        List<Item> result = new ArrayList<Item>();
        if (world == null || center == null || !Double.isFinite(radius) || radius <= 0) return result;
        // Bound the legacy loaded-chunk query as well as invalid/extreme configuration.
        radius = Math.min(radius, 128.0D);
        if (NEARBY != null) {
            try {
                Object entities = NEARBY.invoke(world, center, radius, radius, radius);
                if (entities instanceof Iterable<?>) {
                    for (Object entity : (Iterable<?>) entities) addItem(result, entity);
                    return result;
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { }
        }
        int minX = (int) Math.floor((center.getX() - radius) / 16.0D);
        int maxX = (int) Math.floor((center.getX() + radius) / 16.0D);
        int minZ = (int) Math.floor((center.getZ() - radius) / 16.0D);
        int maxZ = (int) Math.floor((center.getZ() + radius) / 16.0D);
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if (!world.isChunkLoaded(x, z)) continue;
            for (Entity entity : world.getChunkAt(x, z).getEntities()) {
                if (!(entity instanceof Item)) continue;
                Location position = entity.getLocation();
                if (Math.abs(position.getX() - center.getX()) <= radius
                        && Math.abs(position.getY() - center.getY()) <= radius
                        && Math.abs(position.getZ() - center.getZ()) <= radius) addItem(result, entity);
            }
        }
        return result;
    }

    private static void addItem(List<Item> result, Object entity) {
        if (entity instanceof Item && ((Item) entity).isValid() && !((Item) entity).isDead()) result.add((Item) entity);
    }

    /** Modern vanilla break, or a cancelled/manual legacy break without duplicate container loot.
     * Called only for a registered hopper after cancellation checks in the listener.
     */
    public static boolean prepareHopperBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return false;
        if (SUPPRESS_DROPS != null) {
            try { SUPPRESS_DROPS.invoke(event, false); return true; }
            catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
                event.setCancelled(true);
                return false; // Do not guess whether a present API partially succeeded.
            }
        }
        event.setCancelled(true);
        Block block = event.getBlock();
        if (!MaterialCompat.isHopper(block.getType())) return false;
        BlockState state = block.getState();
        Inventory inventory = state instanceof InventoryHolder ? ((InventoryHolder) state).getInventory() : null;
        ItemStack[] contents = inventory == null ? new ItemStack[0]
                : network.paragon.hoppers.service.MiningTransfer.copy(inventory.getContents());
        Material air = MaterialCompat.resolve("AIR");
        if (air == null) return false;
        try {
            if (inventory != null) inventory.clear();
            block.setType(air);
        } catch (RuntimeException | LinkageError failure) {
            if (!MiningCompat.isAir(block.getType())) {
                if (inventory != null) inventory.setContents(contents);
                return false;
            }
        }
        if (!MiningCompat.isAir(block.getType())) {
            if (inventory != null) inventory.setContents(contents);
            return false;
        }
        for (ItemStack item : contents) if (item != null && item.getAmount() > 0 && !MiningCompat.isAir(item.getType())) {
            block.getWorld().dropItemNaturally(block.getLocation(), item);
        }
        return true;
    }
}
