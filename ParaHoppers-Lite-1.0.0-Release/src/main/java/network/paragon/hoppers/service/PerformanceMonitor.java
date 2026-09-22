package network.paragon.hoppers.service;
import network.paragon.hoppers.ParaHoppersLite;
import org.bukkit.Bukkit;
import java.util.concurrent.atomic.AtomicLong;

public final class PerformanceMonitor {
 private final ParaHoppersLite plugin;
 private final AtomicLong collectionCycles=new AtomicLong(), miningCycles=new AtomicLong(), teleportCycles=new AtomicLong();
 private final AtomicLong minedBlocks=new AtomicLong(), transferredItems=new AtomicLong(), collectedItems=new AtomicLong();
 public PerformanceMonitor(ParaHoppersLite p){plugin=p;}
 public void collectionCycle(){collectionCycles.incrementAndGet();}
 public void miningCycle(){miningCycles.incrementAndGet();}
 public void teleportCycle(){teleportCycles.incrementAndGet();}
 public void mined(long n){minedBlocks.addAndGet(n);}
 public void transferred(long n){transferredItems.addAndGet(n);}
 public void collected(long n){collectedItems.addAndGet(n);}
 public long collectionCycles(){return collectionCycles.get();}
 public long miningCycles(){return miningCycles.get();}
 public long teleportCycles(){return teleportCycles.get();}
 public long minedBlocks(){return minedBlocks.get();}
 public long transferredItems(){return transferredItems.get();}
 public long collectedItems(){return collectedItems.get();}
 public double tps(){return plugin.getCompatibility().tps();}
 public int hopperCount(){return plugin.getHopperRegistry().all().size();}
 public int activeMiners(){return (int)plugin.getHopperRegistry().all().stream().filter(plugin.getMiningStateManager()::isEnabled).count();}
}