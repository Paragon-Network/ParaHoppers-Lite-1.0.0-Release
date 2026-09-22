package network.paragon.hoppers.compat.item;

import org.bukkit.ChatColor;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

/**
 * Durable fallback that only uses ItemMeta/lore APIs available on legacy Bukkit.
 * Markers are intentionally machine-readable and survive inventory moves/restarts.
 */
public final class LegacyLoreItemTagAdapter implements ItemTagAdapter {
    private static final String PREFIX = ChatColor.BLACK + "PH:";

    public void setString(ItemMeta meta, String key, String value) { put(meta, key, value); }
    public void setInt(ItemMeta meta, String key, int value) { put(meta, key, Integer.toString(value)); }
    public String getString(ItemMeta meta, String key) { return read(meta, key); }
    public Integer getInt(ItemMeta meta, String key) {
        String value = read(meta, key);
        if (value == null) return null;
        try { return Integer.valueOf(value); } catch (NumberFormatException ignored) { return null; }
    }
    public boolean has(ItemMeta meta, String key) { return read(meta, key) != null; }
    public String name() { return "legacy-lore"; }

    private void put(ItemMeta meta, String key, String value) {
        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
        String marker = marker(key);
        for (int i = lore.size() - 1; i >= 0; i--) {
            if (lore.get(i) != null && lore.get(i).startsWith(marker)) lore.remove(i);
        }
        lore.add(marker + value);
        meta.setLore(lore);
    }

    private String read(ItemMeta meta, String key) {
        if (meta == null || !meta.hasLore() || meta.getLore() == null) return null;
        String marker = marker(key);
        for (String line : meta.getLore()) {
            if (line != null && line.startsWith(marker)) return line.substring(marker.length());
        }
        return null;
    }

    private String marker(String key) { return PREFIX + key + "="; }
}
