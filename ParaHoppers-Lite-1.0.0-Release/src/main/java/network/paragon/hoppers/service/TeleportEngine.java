package network.paragon.hoppers.service;

import network.paragon.hoppers.ParaHoppersLite;
import network.paragon.hoppers.hopper.ParagonHopper;
import network.paragon.hoppers.compat.InventoryCompat;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public final class TeleportEngine {
    private final ParaHoppersLite plugin; private final LinkManager links; private BukkitTask task; private int cursor;
    public TeleportEngine(ParaHoppersLite plugin,LinkManager links){this.plugin=plugin;this.links=links;}
    public void start(){stop();if(!plugin.getConfig().getBoolean("teleport.enabled",true))return;long i=Math.max(1,plugin.getConfig().getLong("teleport.processing-interval-ticks",10));task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,i,i);}
    public void stop(){if(task!=null){task.cancel();task=null;}}
    public void restart(){start();}
    private void tick(){
        List<ParagonHopper> hs=plugin.getHopperRegistry().all().stream().filter(h->links.get(h)!=null).collect(java.util.stream.Collectors.toList());if(hs.isEmpty()){cursor=0;return;}
        int budget=Math.min(hs.size(),Math.max(1,plugin.getConfig().getInt("teleport.max-hoppers-per-cycle",100)));
        for(int i=0;i<budget;i++){if(cursor>=hs.size())cursor=0;process(hs.get(cursor++));}
    }
    private void process(ParagonHopper src){
        ParagonHopper dst=find(links.get(src));if(dst==null)return;
        if(!plugin.getConfig().getBoolean("teleport.allow-cross-world",false)&&!src.world().equals(dst.world()))return;
        World sw=Bukkit.getWorld(src.world()),dw=Bukkit.getWorld(dst.world());if(sw==null||dw==null)return;
        network.paragon.hoppers.compat.WorldCompat.Bounds sb=network.paragon.hoppers.compat.WorldCompat.bounds(sw),db=network.paragon.hoppers.compat.WorldCompat.bounds(dw);
        if(sb==null||db==null||!sb.contains(src.y())||!db.contains(dst.y()))return;
        if(!sw.isChunkLoaded(src.x()>>4,src.z()>>4))return;
        boolean allow=plugin.getConfig().getBoolean("teleport.allow-unloaded-destination",false);
        if(!allow&&!dw.isChunkLoaded(dst.x()>>4,dst.z()>>4))return;
        if(allow&&!dw.isChunkLoaded(dst.x()>>4,dst.z()>>4))dw.getChunkAt(dst.x()>>4,dst.z()>>4).load();
        Inventory a=InventoryCompat.inventory(sw.getBlockAt(src.x(),src.y(),src.z()).getState());
        Inventory b=InventoryCompat.inventory(dw.getBlockAt(dst.x(),dst.y(),dst.z()).getState());
        if(a==null||b==null)return;
        int budget=Math.max(1,plugin.getConfig().getInt("teleport.items-per-transfer",8));
        for(int slot=0;slot<a.getSize()&&budget>0;slot++){
            int moved=InventoryCompat.transferSlot(a,slot,b,budget);
            budget-=moved;
        }
    }
    private ParagonHopper find(String key){if(key==null)return null;return plugin.getHopperRegistry().all().stream().filter(h->h.locationKey().equals(key)).findFirst().orElse(null);}
}
