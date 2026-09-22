package network.paragon.hoppers.service;

import network.paragon.hoppers.ParaHoppersLite;
import network.paragon.hoppers.hopper.HopperType;
import network.paragon.hoppers.compat.InventoryCompat;
import network.paragon.hoppers.hopper.ParagonHopper;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class CollectionEngine {
    private final ParaHoppersLite plugin;
    private BukkitTask task;
    private int cursor;

    public CollectionEngine(ParaHoppersLite plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.getConfig().getBoolean("collection.enabled", true)) return;
        long interval = Math.max(1L, plugin.getConfig().getLong("collection.processing-interval-ticks", 5L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void restart() {
        start();
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("collection.enabled", true)) return;

        double minTps = plugin.getConfig().getDouble("collection.pause-below-tps", 17.0);
        double[] tps = new double[]{plugin.getCompatibility().tps()};
        if (tps.length > 0 && tps[0] < minTps) return;

        List<ParagonHopper> hoppers = plugin.getHopperRegistry().all().stream()
                .filter(h -> h.type() == HopperType.COLLECTOR)
                .collect(java.util.stream.Collectors.toList());
        if (hoppers.isEmpty()) {
            cursor = 0;
            return;
        }

        int budget = Math.max(1, plugin.getConfig().getInt("collection.max-hoppers-per-cycle", 100));
        int count = Math.min(budget, hoppers.size());

        for (int i = 0; i < count; i++) {
            if (cursor >= hoppers.size()) cursor = 0;
            process(hoppers.get(cursor++));
        }
    }

    private void process(ParagonHopper data) {
        World world = Bukkit.getWorld(data.world());
        if (world == null) return;
        network.paragon.hoppers.compat.WorldCompat.Bounds bounds = network.paragon.hoppers.compat.WorldCompat.bounds(world);
        if (bounds == null || !bounds.contains(data.y())) return;
        if (!world.isChunkLoaded(data.x() >> 4, data.z() >> 4)) return;

        Block block = world.getBlockAt(data.x(), data.y(), data.z());
        Inventory inventory = InventoryCompat.inventory(block.getState());
        if (inventory == null) return;

        double radius = Math.max(0.5,
                plugin.getConfig().getDouble("collection.levels." + data.level() + ".radius", data.level()));
        int itemBudget = Math.max(1,
                plugin.getConfig().getInt("collection.max-items-per-hopper-cycle", 16));

        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        Collection<Item> nearby = network.paragon.hoppers.compat.RuntimeCompat.nearbyItems(world, center, radius);

        int processed = 0;

        for (Item entity : nearby) {
            if (processed >= itemBudget) break;
            ItemStack source = entity.getItemStack();
            if (source.getAmount() <= 0) continue;

            int moved = InventoryCompat.addAndCount(inventory, source);
            if (moved >= source.getAmount()) {
                entity.remove();
                processed++;
                continue;
            }

            if (moved > 0) {
                ItemStack remainder = source.clone();
                remainder.setAmount(source.getAmount() - moved);
                entity.setItemStack(remainder);
                processed++;
            }
            // If nothing fit, leave the entity untouched and move on.
        }
    }
}
