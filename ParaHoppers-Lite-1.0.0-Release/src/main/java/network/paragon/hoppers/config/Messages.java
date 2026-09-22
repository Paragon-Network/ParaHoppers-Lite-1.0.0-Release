package network.paragon.hoppers.config;

import network.paragon.hoppers.ParaHoppersLite;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class Messages {
    private final ParaHoppersLite plugin;
    private FileConfiguration config;

    public Messages(ParaHoppersLite plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    public String get(String path) {
        String prefix = color(config.getString("prefix", "&8[&dPara Hoppers&8] "));
        return prefix + color(config.getString(path, "&cMissing message: " + path));
    }

    public String raw(String path) {
        return color(config.getString(path, "&cMissing message: " + path));
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
