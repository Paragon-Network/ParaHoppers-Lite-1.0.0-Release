package network.paragon.hoppers.listener;

import network.paragon.hoppers.ParaHoppersLite;
import network.paragon.hoppers.command.ParaHoppersCommand;
import network.paragon.hoppers.gui.HopperGui;
import network.paragon.hoppers.gui.FilterGui;
import network.paragon.hoppers.compat.InventoryCompat;
import network.paragon.hoppers.hopper.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import java.util.Optional;

public final class HopperListener implements Listener {
    private final ParaHoppersLite plugin; private final ParaHoppersCommand command; private final HopperGui gui;
    public HopperListener(ParaHoppersLite plugin,ParaHoppersCommand command){this.plugin=plugin;this.command=command;this.gui=new HopperGui(plugin);}

    @EventHandler(ignoreCancelled=true) public void onPlace(BlockPlaceEvent e){
        ItemStack item=e.getItemInHand(); if(!plugin.getHopperItemFactory().isParaHopper(item))return;
        if(!e.getPlayer().hasPermission("parahoppers.place")){e.setCancelled(true);e.getPlayer().sendMessage(plugin.getMessages().get("no-permission"));return;}
        HopperType type=plugin.getHopperItemFactory().getType(item); int level=plugin.getHopperItemFactory().getLevel(item);
        plugin.getHopperRegistry().register(ParagonHopper.create(e.getPlayer().getUniqueId(),e.getBlockPlaced().getLocation(),type,level));
        e.getPlayer().sendMessage(plugin.getMessages().get("placed").replace("%type%",type.name()));
        if(plugin.getConfig().getBoolean("visuals.hopper-sounds",false))plugin.getVisuals().sound(e.getPlayer(),network.paragon.hoppers.compat.VisualCompat.Cue.PLACE);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onBreak(BlockBreakEvent e){
        if(e.getBlock().getType()!=Material.HOPPER)return; ParagonHopper h=plugin.getHopperRegistry().get(e.getBlock().getLocation()); if(h==null)return;
        if(!network.paragon.hoppers.compat.RuntimeCompat.prepareHopperBreak(e))return;
        plugin.getLinkManager().removeLocation(h.locationKey());
        plugin.getMiningStateManager().remove(h.locationKey());
        plugin.getQuarryStateManager().remove(h.locationKey());
        plugin.getFilterManager().remove(h.locationKey());
        plugin.getHopperRegistry().remove(e.getBlock().getLocation());
        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),plugin.getHopperItemFactory().create(h.type(),h.level(),1));
        e.getPlayer().sendMessage(plugin.getMessages().get("broken"));
        if(plugin.getConfig().getBoolean("visuals.hopper-sounds",false))plugin.getVisuals().sound(e.getPlayer(),network.paragon.hoppers.compat.VisualCompat.Cue.BREAK);
    }
    @EventHandler(ignoreCancelled=true) public void onInteract(PlayerInteractEvent e){
        if(!network.paragon.hoppers.compat.RuntimeCompat.mainHand(e)||!"RIGHT_CLICK_BLOCK".equals(e.getAction().name()))return;
        Block b=e.getClickedBlock();if(b==null||b.getType()!=Material.HOPPER)return;ParagonHopper h=plugin.getHopperRegistry().get(b.getLocation());if(h==null)return;
        e.setCancelled(true);Player p=e.getPlayer();
        String linkSource=command.getLinkSource(p.getUniqueId());
        if(linkSource!=null){
            ParagonHopper source=plugin.getHopperRegistry().all().stream().filter(x->x.locationKey().equals(linkSource)).findFirst().orElse(null);
            if(source==null){command.clearLink(p.getUniqueId());return;}
            if(source.locationKey().equals(h.locationKey())){p.sendMessage(plugin.getMessages().get("link-self"));return;}
            if(plugin.getConfig().getBoolean("teleport.owner-only-linking",true)&&(!source.owner().equals(p.getUniqueId())||!h.owner().equals(p.getUniqueId()))&&!p.hasPermission("parahoppers.admin")){p.sendMessage(plugin.getMessages().get("link-owner"));return;}
            if(!plugin.getConfig().getBoolean("teleport.allow-cross-world",false)&&!source.world().equals(h.world())){p.sendMessage(plugin.getMessages().get("link-cross-world"));return;}
            plugin.getLinkManager().set(source,h);command.clearLink(p.getUniqueId());
            p.sendMessage(plugin.getMessages().get("link-created").replace("%world%",h.world()).replace("%x%",""+h.x()).replace("%y%",""+h.y()).replace("%z%",""+h.z()));return;
        }
        if(command.isInspecting(p.getUniqueId())){
            String owner=Optional.ofNullable(Bukkit.getOfflinePlayer(h.owner()).getName()).orElse("Unknown");
            p.sendMessage("§d§lPara Hopper §8[§fLite§8]");p.sendMessage("§7Type: §f"+h.type());p.sendMessage("§7Level: §f"+h.level());
            p.sendMessage("§7Owner: §f"+owner);p.sendMessage("§7UUID: §f"+h.owner());
            p.sendMessage("§7Location: §f"+h.world()+" "+h.x()+", "+h.y()+", "+h.z());command.disableInspect(p.getUniqueId());return;
        }
        gui.open(p,h);
    }

    @EventHandler public void onFilterGui(InventoryClickEvent e){
        if(!(InventoryCompat.holder(e) instanceof FilterGui.Holder))return;FilterGui.Holder holder=(FilterGui.Holder)InventoryCompat.holder(e);
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player))return;Player p=(Player)e.getWhoClicked();
        ParagonHopper h=plugin.getHopperRegistry().all().stream().filter(x->x.locationKey().equals(holder.key())).findFirst().orElse(null);
        if(h==null){p.closeInventory();return;}
        boolean admin=p.hasPermission("parahoppers.admin");
        if(!h.owner().equals(p.getUniqueId())&&!admin){p.sendMessage("§cYou do not own this Para Hopper.");p.closeInventory();return;}
        FilterGui gui=new FilterGui(plugin);
        int slot=e.getRawSlot();
        if(slot==45){plugin.getFilterManager().toggleMode(h);gui.open(p,h);return;}
        if(slot==49){plugin.getFilterManager().clear(h);gui.open(p,h);return;}
        if(slot==47){if(plugin.getConfig().getBoolean("visuals.gui-sounds",true))plugin.getVisuals().sound(p,network.paragon.hoppers.compat.VisualCompat.Cue.DENIED);p.sendMessage("§cBedrock protection is permanent and cannot be removed.");return;}
        if(slot==53){new HopperGui(plugin).open(p,h);return;}
        if(slot>=0&&slot<45&&e.getCurrentItem()!=null&&!network.paragon.hoppers.compat.MiningCompat.isAir(e.getCurrentItem().getType())){plugin.getFilterManager().toggleItem(h,e.getCurrentItem().getType());gui.open(p,h);return;}
        // Shift-click an item from the player's inventory to add/remove it from the filter.
        if(e.isShiftClick()&&InventoryCompat.isPlayerInventoryClick(e)&&e.getCurrentItem()!=null&&!network.paragon.hoppers.compat.MiningCompat.isAir(e.getCurrentItem().getType())){plugin.getFilterManager().toggleItem(h,e.getCurrentItem().getType());gui.open(p,h);}
    }

    @EventHandler public void onGui(InventoryClickEvent e){
        if(!(InventoryCompat.holder(e) instanceof HopperGui.Holder))return;HopperGui.Holder holder=(HopperGui.Holder)InventoryCompat.holder(e);e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player))return;Player p=(Player)e.getWhoClicked();
        if (e.getRawSlot() == 29) {
        plugin.getHopperRegistry().all().stream()
                .filter(x -> x.locationKey().equals(holder.key()))
                .findFirst()
                .ifPresent(h -> new FilterGui(plugin).open(p, h));
        return;
    }
    if (e.getRawSlot() != 15 && e.getRawSlot() != 31) {
        return;
    }
        ParagonHopper h=plugin.getHopperRegistry().all().stream().filter(x->x.locationKey().equals(holder.key())).findFirst().orElse(null);if(h==null){p.closeInventory();return;}
        if(e.getRawSlot()==31){
            if(!p.getUniqueId().equals(h.owner())&&!p.hasPermission("parahoppers.admin")){p.sendMessage(plugin.getMessages().get("not-owner"));return;}
            boolean enabled=plugin.getMiningStateManager().toggle(h);
            p.sendMessage(plugin.getMessages().get(enabled?"miner-enabled":"miner-disabled"));
            gui.open(p,h);
            return;
        }
        if(!p.getUniqueId().equals(h.owner())&&!p.hasPermission("parahoppers.admin")){p.sendMessage(plugin.getMessages().get("not-owner"));return;}
        int max=plugin.getConfig().getInt("hopper.max-level",5);if(h.level()>=max){p.sendMessage(plugin.getMessages().get("max-level"));return;}
        int next=h.level()+1;
        String path="upgrades.levels."+next;
        int xpCost=plugin.getConfig().getInt(path+".experience-levels",0);
        if(xpCost<=0){
            p.sendMessage("§8[§dPara Hoppers§8] §cUpgrade XP cost is not configured correctly.");
            return;
        }
        if(p.getLevel()<xpCost){
            p.sendMessage(plugin.getMessages().get("upgrade-not-enough-xp")
                    .replace("%cost%",String.valueOf(xpCost))
                    .replace("%current%",String.valueOf(p.getLevel())));
            return;
        }
        p.setLevel(p.getLevel()-xpCost);
        ParagonHopper u=new ParagonHopper(h.id(),h.owner(),h.world(),h.x(),h.y(),h.z(),h.type(),next,h.createdAt());
        plugin.getHopperRegistry().update(u);
        p.sendMessage(plugin.getMessages().get("upgrade-success")
                .replace("%level%",String.valueOf(next))
                .replace("%cost%",String.valueOf(xpCost)));
        gui.open(p,u);
    }
    @EventHandler public void onGuiDrag(InventoryDragEvent e){
        InventoryHolder holder=InventoryCompat.holder(e);
        if(holder instanceof FilterGui.Holder || holder instanceof HopperGui.Holder)e.setCancelled(true);
    }

}
