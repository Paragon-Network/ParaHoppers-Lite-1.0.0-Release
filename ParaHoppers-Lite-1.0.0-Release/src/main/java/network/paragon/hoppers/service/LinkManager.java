package network.paragon.hoppers.service;

import network.paragon.hoppers.ParaHoppersLite;
import network.paragon.hoppers.hopper.ParagonHopper;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.util.*;

public final class LinkManager {
    private final ParaHoppersLite plugin;
    private final File file;
    private final Map<String,String> links=new HashMap<>();
    public LinkManager(ParaHoppersLite plugin){this.plugin=plugin;this.file=new File(plugin.getDataFolder(),"links.yml");load();}
    public void set(ParagonHopper source,ParagonHopper dest){links.put(source.locationKey(),dest.locationKey());save();}
    public String get(ParagonHopper source){return links.get(source.locationKey());}
    public boolean remove(ParagonHopper source){boolean r=links.remove(source.locationKey())!=null;if(r)save();return r;}
    public void removeLocation(String key){links.remove(key);links.entrySet().removeIf(e->e.getValue().equals(key));save();}
    public void load(){links.clear();if(!file.exists())return;YamlConfiguration y=YamlConfiguration.loadConfiguration(file);org.bukkit.configuration.ConfigurationSection sec=y.getConfigurationSection("links");if(sec!=null)for(String k:sec.getKeys(false))links.put(k.replace("|",":"),sec.getString(k));}
    public void save(){YamlConfiguration y=new YamlConfiguration();for(Map.Entry<String,String> e:links.entrySet())y.set("links."+e.getKey().replace(":","|"),e.getValue());try{y.save(file);}catch(IOException e){plugin.getLogger().severe("Could not save links.yml: "+e.getMessage());}}
}
