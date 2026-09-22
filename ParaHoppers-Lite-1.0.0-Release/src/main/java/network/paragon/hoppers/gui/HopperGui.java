package network.paragon.hoppers.gui;
import network.paragon.hoppers.ParaHoppersLite;
import network.paragon.hoppers.hopper.ParagonHopper;
import network.paragon.hoppers.compat.MaterialCompat;
import network.paragon.hoppers.compat.InventoryCompat;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.io.File;
import java.util.*;

public final class HopperGui {
 private final ParaHoppersLite plugin;
 public HopperGui(ParaHoppersLite plugin){this.plugin=plugin;}
 public void open(Player p,ParagonHopper h){
  YamlConfiguration y=YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(),"menus/hopper-menu.yml"));
  Inventory inv=InventoryCompat.create(new Holder(h.locationKey()),Math.max(1,Math.min(6,y.getInt("rows",4)))*9,color(repl(y.getString("title","&8Para Hopper"),p,h)));
  put(inv,y,"items.info",p,h);
  if(h.level()>=plugin.getConfig().getInt("hopper.max-level",5))put(inv,y,"items.max-level",p,h);else put(inv,y,"items.upgrade",p,h);
  put(inv,y,"items.mining",p,h);
  put(inv,y,"items.filter",p,h);
  p.openInventory(inv);
  if(plugin.getConfig().getBoolean("visuals.gui-sounds",true))plugin.getVisuals().sound(p,network.paragon.hoppers.compat.VisualCompat.Cue.CLICK);
 }
 private void put(Inventory inv,YamlConfiguration y,String path,Player p,ParagonHopper h){
  ConfigurationSection c=y.getConfigurationSection(path);if(c==null)return;
  Material m=MaterialCompat.resolve(c.getString("material","BARRIER"));if(m==null)m=MaterialCompat.resolve("CHEST");
  ItemStack it=new ItemStack(m);ItemMeta meta=it.getItemMeta();meta.setDisplayName(color(repl(c.getString("name","&cUnavailable"),p,h)));
  List<String> lore=new ArrayList<>();for(String line:c.getStringList("lore"))lore.add(color(repl(line,p,h)));meta.setLore(lore);it.setItemMeta(meta);
  int slot=c.getInt("slot",-1);if(slot>=0&&slot<inv.getSize())inv.setItem(slot,it);
 }
 private String repl(String t,Player p,ParagonHopper h){
  if(t==null)return "";int max=plugin.getConfig().getInt("hopper.max-level",5),next=Math.min(max,h.level()+1);
  String owner=Bukkit.getOfflinePlayer(h.owner()).getName();if(owner==null)owner="Unknown";
  double r=plugin.getConfig().getDouble("collection.levels."+h.level()+".radius",h.level()),nr=plugin.getConfig().getDouble("collection.levels."+next+".radius",next);
  return t.replace("%type%",h.type().name()).replace("%owner%",owner).replace("%owner_uuid%",h.owner().toString())
   .replace("%level%",""+h.level()).replace("%max_level%",""+max).replace("%next_level%",""+next)
   .replace("%xp_cost%",""+plugin.getConfig().getInt("upgrades.levels."+next+".experience-levels",0)).replace("%player_levels%",""+p.getLevel())
   .replace("%collection_radius%",fmt(r)).replace("%next_collection_radius%",fmt(nr))
   .replace("%collection_status%",plugin.getConfig().getBoolean("collection.enabled",true)?"&aEnabled":"&cDisabled")
   .replace("%mining_status%",plugin.getMiningStateManager().isEnabled(h)?"&aEnabled":"&cDisabled")
   .replace("%mining_radius%",""+plugin.getConfig().getInt("mining.levels."+h.level()+".radius",1))
   .replace("%mining_area%",miningArea(h))
   .replace("%mining_layer_blocks%",""+miningLayerBlocks(h))
   .replace("%mining_depth%","Continuous / layer-by-layer")
   .replace("%mining_blocks%",""+plugin.getConfig().getInt("mining.levels."+h.level()+".blocks-per-cycle",1))
   .replace("%filter_mode%",plugin.getFilterManager().mode(h).name())
   .replace("%filter_count%",""+plugin.getFilterManager().items(h).size())
   .replace("%world%",h.world()).replace("%x%",""+h.x()).replace("%y%",""+h.y()).replace("%z%",""+h.z());
 }
 private String miningArea(ParagonHopper h){int r=plugin.getConfig().getInt("mining.levels."+h.level()+".radius",1);int side=r*2+1;return side+"x"+side;}
 private int miningLayerBlocks(ParagonHopper h){int r=plugin.getConfig().getInt("mining.levels."+h.level()+".radius",1);int side=r*2+1;return side*side;}
 private String fmt(double d){return d==(long)d?Long.toString((long)d):Double.toString(d);}
 private String color(String s){return s.replace('&','§');}
 public static final class Holder implements InventoryHolder{
  private final String key;
  public Holder(String key){this.key=key;}
  public String key(){return key;}
  public Inventory getInventory(){return null;}
 }
}
