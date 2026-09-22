package network.paragon.hoppers.service;

import network.paragon.hoppers.ParaHoppersLite;
import network.paragon.hoppers.hopper.ParagonHopper;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.util.*;

public final class MiningStateManager {
    private final ParaHoppersLite plugin; private final File file; private final Set<String> enabled=new HashSet<>();
    public MiningStateManager(ParaHoppersLite plugin){this.plugin=plugin;file=new File(plugin.getDataFolder(),"mining.yml");load();}
    public boolean isEnabled(ParagonHopper h){return enabled.contains(h.locationKey());}
    public boolean toggle(ParagonHopper h){if(!enabled.add(h.locationKey()))enabled.remove(h.locationKey());save();return isEnabled(h);}
    public void remove(String key){if(enabled.remove(key))save();}
    public void load(){enabled.clear();if(!file.exists())return;YamlConfiguration y=YamlConfiguration.loadConfiguration(file);enabled.addAll(y.getStringList("enabled"));}
    public void save(){YamlConfiguration y=new YamlConfiguration();y.set("enabled",new ArrayList<>(enabled));try{y.save(file);}catch(IOException e){plugin.getLogger().severe("Could not save mining.yml: "+e.getMessage());}}
}
