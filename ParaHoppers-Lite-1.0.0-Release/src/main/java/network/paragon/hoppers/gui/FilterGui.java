package network.paragon.hoppers.gui;
import network.paragon.hoppers.ParaHoppersLite;
import network.paragon.hoppers.hopper.ParagonHopper;
import network.paragon.hoppers.service.FilterManager;
import network.paragon.hoppers.compat.MaterialCompat;
import network.paragon.hoppers.compat.InventoryCompat;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class FilterGui {
 public static final class Holder implements InventoryHolder {
  private final String key;
  public Holder(String key){this.key=key;}
  public String key(){return key;}
  public Inventory getInventory(){return null;}
 }
 private final ParaHoppersLite plugin; public FilterGui(ParaHoppersLite p){plugin=p;}
 public void open(Player p,ParagonHopper h){
  Inventory inv=InventoryCompat.create(new Holder(h.locationKey()),54,"Para Hopper - Filter");
  inv.setItem(47,item(MaterialCompat.resolve("BEDROCK"),"§c§lBedrock Protection",java.util.Arrays.asList("§7Always blocked from mining.","§cLocked — cannot be removed.")));
  inv.setItem(45,item(first("COMPARATOR","REDSTONE_COMPARATOR"),"§eFilter Mode",java.util.Arrays.asList("§7Current: §f"+plugin.getFilterManager().mode(h),"§eClick to switch whitelist/blacklist.")));
  inv.setItem(49,item(first("BARRIER","CHEST"),"§cClear Filter",java.util.Arrays.asList("§7Removes every filter entry.","§7An empty filter accepts everything.")));
  inv.setItem(53,item(MaterialCompat.resolve("ARROW"),"§aBack",java.util.Arrays.asList("§7Return to hopper controls.")));
  int slot=0;for(Material m:plugin.getFilterManager().items(h)){while(slot==45||slot==49||slot==53)slot++;if(slot>=45)break;inv.setItem(slot++,item(m,"§f"+pretty(m),java.util.Arrays.asList("§cClick to remove.")));}
  p.openInventory(inv);
  if(plugin.getConfig().getBoolean("visuals.gui-sounds",true))plugin.getVisuals().sound(p,network.paragon.hoppers.compat.VisualCompat.Cue.CLICK);
 }
 private Material first(String... names){for(String n:names){Material m=MaterialCompat.resolve(n);if(m!=null)return m;}return MaterialCompat.resolve("STONE");}
 private ItemStack item(Material m,String n,List<String>l){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(n);meta.setLore(l);i.setItemMeta(meta);return i;}
 private String pretty(Material m){String s=m.name().toLowerCase(Locale.ROOT).replace('_',' ');return Character.toUpperCase(s.charAt(0))+s.substring(1);}
}