package network.paragon.hoppers.compat.item;

import org.bukkit.inventory.meta.ItemMeta;

/** Version-neutral storage boundary for ParaHoppers item identity. */
public interface ItemTagAdapter {
    void setString(ItemMeta meta, String key, String value);
    void setInt(ItemMeta meta, String key, int value);
    String getString(ItemMeta meta, String key);
    Integer getInt(ItemMeta meta, String key);
    boolean has(ItemMeta meta, String key);
    String name();
}
