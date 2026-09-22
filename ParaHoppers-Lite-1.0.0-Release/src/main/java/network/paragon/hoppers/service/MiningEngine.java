package network.paragon.hoppers.service;

import network.paragon.hoppers.ParaHoppersLite;
import network.paragon.hoppers.hopper.ParagonHopper;
import network.paragon.hoppers.compat.MaterialCompat;
import network.paragon.hoppers.compat.MiningCompat;
import network.paragon.hoppers.compat.WorldCompat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import java.util.List;
import java.util.Set;

public final class MiningEngine {
    private final ParaHoppersLite plugin;
    private final MiningStateManager states;
    private final QuarryStateManager quarry;
    private BukkitTask task;
    private int cursor;
    private boolean faulted;

    public MiningEngine(ParaHoppersLite plugin, MiningStateManager states, QuarryStateManager quarry) {
        this.plugin = plugin;
        this.states = states;
        this.quarry = quarry;
    }
    public void start() {
        stop();
        // An unknown transaction outcome needs inspection and a server restart.
        if (faulted || !plugin.getConfig().getBoolean("mining.enabled", true)) return;
        long interval = Math.max(1, plugin.getConfig().getLong("mining.processing-interval-ticks", 20));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }
    public void stop() { if (task != null) { task.cancel(); task = null; } }
    public void restart() { start(); }

    private void tick() {
        if (plugin.getCompatibility().tps() < plugin.getConfig().getDouble("mining.pause-below-tps", 17)) return;
        plugin.getPerformanceMonitor().miningCycle();
        List<ParagonHopper> hoppers = plugin.getHopperRegistry().all().stream().filter(states::isEnabled).collect(java.util.stream.Collectors.toList());
        if (hoppers.isEmpty()) { cursor = 0; return; }
        int count = Math.max(1, Math.min(plugin.getConfig().getInt("mining.max-hoppers-per-cycle", 40), hoppers.size()));
        for (int i = 0; i < count; i++) {
            if (cursor >= hoppers.size()) cursor = 0;
            ParagonHopper hopper = hoppers.get(cursor++);
            try { mine(hopper); }
            catch (RuntimeException | LinkageError failure) {
                faulted = true;
                stop();
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Mining stopped at " + hopper.locationKey()
                        + "; inspect block, inventory and quarry cursor before restarting the server.", failure);
                return;
            }
        }
    }

    private void mine(ParagonHopper hopper) {
        World world = Bukkit.getWorld(hopper.world());
        WorldCompat.Bounds bounds = WorldCompat.bounds(world);
        if (bounds == null || !bounds.contains(hopper.y())
                || !world.isChunkLoaded(hopper.x() >> 4, hopper.z() >> 4)) return;
        if (plugin.getConfig().getBoolean("mining.require-owner-online", false)
                && Bukkit.getPlayer(hopper.owner()) == null) return;
        org.bukkit.block.BlockState hopperState = world.getBlockAt(hopper.x(), hopper.y(), hopper.z()).getState();
        if (!(hopperState instanceof Hopper)) return;
        Inventory inventory = ((Hopper) hopperState).getInventory();
        // Bound invalid configuration arithmetic; shipped radii are unchanged.
        int radius = Math.max(0, Math.min(32, plugin.getConfig().getInt("mining.levels." + hopper.level() + ".radius", 1)));
        int budget = Math.max(1, plugin.getConfig().getInt("mining.levels." + hopper.level() + ".blocks-per-cycle", 1));
        int scans = Math.max(1, Math.min(65536, plugin.getConfig().getInt("mining.max-scanned-blocks-per-hopper-cycle", 256)));
        Set<Material> allowed = materials("mining.allowed-materials");
        Set<Material> blocked = materials("mining.blocked-materials");
        Material toolType = MaterialCompat.resolve(plugin.getConfig().getString("mining.tool-material", "DIAMOND_PICKAXE"));
        if (toolType == null || MiningCompat.isAir(toolType)) toolType = MaterialCompat.resolve("DIAMOND_PICKAXE");
        if (toolType == null) return;
        ItemStack tool = new ItemStack(toolType);
        QuarryStateManager.Cursor saved = quarry.get(hopper);
        int y = saved.y();
        int index = Math.max(0, saved.index());
        boolean aboveDone = saved.aboveDone();
        if (y >= bounds.max) { y = bounds.max - 1; index = 0; }
        if (aboveDone && y < bounds.min) return;
        if (aboveDone && y >= hopper.y()) { y = hopper.y() - 1; index = 0; }
        int side = radius * 2 + 1;
        int total = side * side;
        while (budget > 0 && scans > 0) {
            if (!aboveDone && (!plugin.getConfig().getBoolean("mining.mine-above", true) || y <= hopper.y())) {
                aboveDone = true;
                y = hopper.y() - 1;
                index = 0;
            }
            if (y < bounds.min) break;
            if (index >= total) { index = 0; y--; continue; }
            int dx = index % side - radius;
            int dz = index / side - radius;
            long targetX = (long) hopper.x() + dx;
            long targetZ = (long) hopper.z() + dz;
            if (targetX < Integer.MIN_VALUE || targetX > Integer.MAX_VALUE
                    || targetZ < Integer.MIN_VALUE || targetZ > Integer.MAX_VALUE) break;
            int x = (int) targetX;
            int z = (int) targetZ;
            // Pause at this candidate. Do not load the footprint's adjacent chunks.
            if (!world.isChunkLoaded(x >> 4, z >> 4)) break;
            Block block = world.getBlockAt(x, y, z);
            index++;
            scans--;
            Material material = block.getType();
            if (MiningCompat.isAir(material) || MaterialCompat.isBedrock(material) || MiningCompat.matches(blocked, material)) continue;
            if (MaterialCompat.isProtectedContainer(material) || block.getState() instanceof InventoryHolder) continue;
            if (MaterialCompat.isFluid(material)) {
                if (!MiningCompat.remove(block, material)) { index--; break; }
                plugin.getPerformanceMonitor().mined(1);
                budget--;
                continue;
            }
            if (!MiningCompat.matches(allowed, material)) continue;
            List<ItemStack> drops = MiningCompat.drops(block, tool);
            if (drops == null) { index--; break; }
            if (drops.stream().anyMatch(drop -> !plugin.getFilterManager().accepts(hopper, drop.getType()))) continue;
            ItemStack[] before = MiningTransfer.copy(inventory.getContents());
            ItemStack[] after = MiningTransfer.plan(before, inventory.getMaxStackSize(), drops);
            if (after == null) { index--; break; }
            boolean removed = MiningTransfer.commit(inventory, before, after, () -> {
                if (plugin.getConfig().getBoolean("visuals.mining-effects", true)) plugin.getVisuals().blockBreak(block);
                return MiningCompat.remove(block, material);
            });
            if (!removed) { index--; break; }
            plugin.getPerformanceMonitor().mined(1);
            budget--;
        }
        quarry.set(hopper, new QuarryStateManager.Cursor(y, index, aboveDone));
    }
    private Set<Material> materials(String path) { return MaterialCompat.resolveAll(plugin.getConfig().getStringList(path)); }
}
