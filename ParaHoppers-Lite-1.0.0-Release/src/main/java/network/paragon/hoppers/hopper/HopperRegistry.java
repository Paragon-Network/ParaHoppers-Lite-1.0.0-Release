package network.paragon.hoppers.hopper;

import network.paragon.hoppers.ParaHoppersLite;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class HopperRegistry {
    private final ParaHoppersLite plugin;
    private final Map<String, ParagonHopper> byLocation = new HashMap<>();
    private final File file;

    public HopperRegistry(ParaHoppersLite plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "hoppers.yml");
    }

    public void register(ParagonHopper hopper) {
        byLocation.put(hopper.locationKey(), hopper);
        if (plugin.getConfig().getBoolean("storage.save-on-change", true)) save();
    }

    public ParagonHopper get(Location location) {
        return byLocation.get(key(location));
    }

    public ParagonHopper remove(Location location) {
        ParagonHopper removed = byLocation.remove(key(location));
        if (removed != null && plugin.getConfig().getBoolean("storage.save-on-change", true)) save();
        return removed;
    }

    public void update(ParagonHopper hopper) {
        byLocation.put(hopper.locationKey(), hopper);
        if (plugin.getConfig().getBoolean("storage.save-on-change", true)) save();
    }

    public Collection<ParagonHopper> all() {
        return Collections.unmodifiableCollection(byLocation.values());
    }

    public int size() {
        return byLocation.size();
    }

    private String key(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":"
                + location.getBlockY() + ":" + location.getBlockZ();
    }

    public void load() {
        byLocation.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("hoppers");
        if (section == null) return;

        for (String idText : section.getKeys(false)) {
            try {
                ConfigurationSection h = section.getConfigurationSection(idText);
                if (h == null) continue;

                ParagonHopper hopper = new ParagonHopper(
                        UUID.fromString(idText),
                        UUID.fromString(h.getString("owner")),
                        h.getString("world"),
                        h.getInt("x"),
                        h.getInt("y"),
                        h.getInt("z"),
                        HopperType.valueOf(h.getString("type", "COLLECTOR")),
                        h.getInt("level", 1),
                        h.getLong("created-at", System.currentTimeMillis())
                );
                byLocation.put(hopper.locationKey(), hopper);
            } catch (Exception ex) {
                plugin.getLogger().warning("Skipped invalid hopper entry " + idText + ": " + ex.getMessage());
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (ParagonHopper hopper : byLocation.values()) {
            String path = "hoppers." + hopper.id();
            yaml.set(path + ".owner", hopper.owner().toString());
            yaml.set(path + ".world", hopper.world());
            yaml.set(path + ".x", hopper.x());
            yaml.set(path + ".y", hopper.y());
            yaml.set(path + ".z", hopper.z());
            yaml.set(path + ".type", hopper.type().name());
            yaml.set(path + ".level", hopper.level());
            yaml.set(path + ".created-at", hopper.createdAt());
        }

        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save hoppers.yml: " + ex.getMessage());
        }
    }
}
