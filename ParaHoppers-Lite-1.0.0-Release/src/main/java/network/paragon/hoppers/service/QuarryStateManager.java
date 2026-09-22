package network.paragon.hoppers.service;
import network.paragon.hoppers.ParaHoppersLite;
import network.paragon.hoppers.hopper.ParagonHopper;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.util.*;

public final class QuarryStateManager {
 public static final class Cursor {
  private final int y,index;private final boolean aboveDone;
  public Cursor(int y,int index,boolean aboveDone){this.y=y;this.index=index;this.aboveDone=aboveDone;}
  public int y(){return y;}public int index(){return index;}public boolean aboveDone(){return aboveDone;}
  @Override public boolean equals(Object other){if(!(other instanceof Cursor))return false;Cursor c=(Cursor)other;return y==c.y&&index==c.index&&aboveDone==c.aboveDone;}
  @Override public int hashCode(){return Objects.hash(y,index,aboveDone);}
  @Override public String toString(){return "Cursor[y="+y+", index="+index+", aboveDone="+aboveDone+"]";}
 }
 private final ParaHoppersLite plugin; private final File file; private final Map<String,Cursor> cursors=new HashMap<>();
 public QuarryStateManager(ParaHoppersLite p){plugin=p;file=new File(p.getDataFolder(),"quarry-state.yml");load();}
 public Cursor get(ParagonHopper h){long start=(long)h.y()+Math.max(0,plugin.getConfig().getInt("mining.max-blocks-above",8));return cursors.getOrDefault(h.locationKey(),new Cursor((int)Math.min(Integer.MAX_VALUE,start),0,false));}
 public void set(ParagonHopper h,Cursor c){if(!c.equals(cursors.put(h.locationKey(),c)))save();}
 public void remove(String key){if(cursors.remove(key)!=null)save();}
 public void reset(ParagonHopper h){cursors.remove(h.locationKey());save();}
 public void load(){cursors.clear();if(!file.exists())return;YamlConfiguration y=YamlConfiguration.loadConfiguration(file);org.bukkit.configuration.ConfigurationSection sec=y.getConfigurationSection("states");if(sec==null)return;for(String k:sec.getKeys(false)){String key=k.replace("|",":");cursors.put(key,new Cursor(sec.getInt(k+".y"),sec.getInt(k+".index"),sec.getBoolean(k+".above-done")));}}
 public void save(){YamlConfiguration y=new YamlConfiguration();for(Map.Entry<String,Cursor> e:cursors.entrySet()){String k="states."+e.getKey().replace(":","|");y.set(k+".y",e.getValue().y());y.set(k+".index",e.getValue().index());y.set(k+".above-done",e.getValue().aboveDone());}try{y.save(file);}catch(IOException e){plugin.getLogger().severe("Could not save quarry-state.yml: "+e.getMessage());}}
}
