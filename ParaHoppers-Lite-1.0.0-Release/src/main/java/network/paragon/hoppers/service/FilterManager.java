package network.paragon.hoppers.service;
import network.paragon.hoppers.ParaHoppersLite;
import network.paragon.hoppers.hopper.ParagonHopper;
import network.paragon.hoppers.compat.MaterialCompat;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.util.*;

public final class FilterManager {
 public enum Mode { WHITELIST, BLACKLIST }
 private final ParaHoppersLite plugin; private final File file;
 private final Map<String,Mode> modes=new HashMap<>(); private final Map<String,Set<Material>> filters=new HashMap<>();
 public FilterManager(ParaHoppersLite p){plugin=p;file=new File(p.getDataFolder(),"filters.yml");load();}
 public Mode mode(ParagonHopper h){return modes.getOrDefault(h.locationKey(),Mode.WHITELIST);}
 public Set<Material> items(ParagonHopper h){return Collections.unmodifiableSet(filters.getOrDefault(h.locationKey(),java.util.Collections.emptySet()));}
 public boolean accepts(ParagonHopper h,Material m){if(MaterialCompat.isBedrock(m))return false;Set<Material>s=filters.getOrDefault(h.locationKey(),java.util.Collections.emptySet());if(s.isEmpty())return true;return mode(h)==Mode.WHITELIST?s.contains(m):!s.contains(m);}
 public Mode toggleMode(ParagonHopper h){Mode n=mode(h)==Mode.WHITELIST?Mode.BLACKLIST:Mode.WHITELIST;modes.put(h.locationKey(),n);save();return n;}
 public boolean toggleItem(ParagonHopper h,Material m){if(MaterialCompat.isBedrock(m))return false;Set<Material>s=filters.computeIfAbsent(h.locationKey(),k->new LinkedHashSet<>());boolean added=s.add(m);if(!added)s.remove(m);save();return added;}
 public void clear(ParagonHopper h){filters.remove(h.locationKey());save();}
 public void remove(String key){modes.remove(key);filters.remove(key);save();}
 private String safe(String k){return Base64.getUrlEncoder().withoutPadding().encodeToString(k.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 private String unsafe(String k){return new String(Base64.getUrlDecoder().decode(k),java.nio.charset.StandardCharsets.UTF_8);}
 public void load(){modes.clear();filters.clear();if(!file.exists())return;YamlConfiguration y=YamlConfiguration.loadConfiguration(file);org.bukkit.configuration.ConfigurationSection sec=y.getConfigurationSection("hoppers");if(sec==null)return;for(String sk:sec.getKeys(false)){String k;try{k=unsafe(sk);}catch(Exception e){continue;}try{modes.put(k,Mode.valueOf(sec.getString(sk+".mode","WHITELIST")));}catch(Exception ignored){}Set<Material> set=new LinkedHashSet<>();for(String n:sec.getStringList(sk+".items")){Material m=MaterialCompat.resolve(n);if(m!=null)set.add(m);}if(!set.isEmpty())filters.put(k,set);}}
 public void save(){YamlConfiguration y=new YamlConfiguration();Set<String>keys=new HashSet<>();keys.addAll(modes.keySet());keys.addAll(filters.keySet());for(String k:keys){String sk="hoppers."+safe(k);y.set(sk+".mode",modes.getOrDefault(k,Mode.WHITELIST).name());y.set(sk+".items",filters.getOrDefault(k,java.util.Collections.emptySet()).stream().map(Material::name).collect(java.util.stream.Collectors.toList()));}try{y.save(file);}catch(IOException e){plugin.getLogger().severe("Could not save filters.yml: "+e.getMessage());}}
}