package network.paragon.hoppers.compat.item;

import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.meta.ItemMeta;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * PDC implementation loaded through reflection so shared classes do not hard-link
 * NamespacedKey/PersistentDataContainer on servers where those classes do not exist.
 * Every write is mirrored to the legacy lore fallback for portability/downgrades.
 */
public final class ReflectivePdcItemTagAdapter implements ItemTagAdapter {
    private final Plugin plugin;
    private final LegacyLoreItemTagAdapter fallback = new LegacyLoreItemTagAdapter();
    private final Constructor<?> keyConstructor;
    private final Method getContainer;
    private final Method set;
    private final Method get;
    private final Object stringType;
    private final Object integerType;

    public ReflectivePdcItemTagAdapter(Plugin plugin) throws ReflectiveOperationException {
        this.plugin = plugin;
        Class<?> keyClass = Class.forName("org.bukkit.NamespacedKey");
        Class<?> pdtClass = Class.forName("org.bukkit.persistence.PersistentDataType");
        Class<?> containerClass = Class.forName("org.bukkit.persistence.PersistentDataContainer");
        keyConstructor = keyClass.getConstructor(Plugin.class, String.class);
        getContainer = ItemMeta.class.getMethod("getPersistentDataContainer");
        set = containerClass.getMethod("set", keyClass, pdtClass, Object.class);
        get = containerClass.getMethod("get", keyClass, pdtClass);
        Field stringField = pdtClass.getField("STRING");
        Field integerField = pdtClass.getField("INTEGER");
        stringType = stringField.get(null);
        integerType = integerField.get(null);
    }

    public void setString(ItemMeta meta, String key, String value) {
        fallback.setString(meta, key, value);
        write(meta, key, stringType, value);
    }
    public void setInt(ItemMeta meta, String key, int value) {
        fallback.setInt(meta, key, value);
        write(meta, key, integerType, Integer.valueOf(value));
    }
    public String getString(ItemMeta meta, String key) {
        Object value = read(meta, key, stringType);
        return value instanceof String ? (String) value : fallback.getString(meta, key);
    }
    public Integer getInt(ItemMeta meta, String key) {
        Object value = read(meta, key, integerType);
        return value instanceof Integer ? (Integer) value : fallback.getInt(meta, key);
    }
    public boolean has(ItemMeta meta, String key) { return getString(meta, key) != null || getInt(meta, key) != null; }
    public String name() { return "reflective-pdc+legacy-lore"; }

    private Object namespacedKey(String key) throws ReflectiveOperationException {
        return keyConstructor.newInstance(plugin, key);
    }
    private void write(ItemMeta meta, String key, Object type, Object value) {
        try { set.invoke(getContainer.invoke(meta), namespacedKey(key), type, value); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) { /* fallback was already written */ }
    }
    private Object read(ItemMeta meta, String key, Object type) {
        try { return get.invoke(getContainer.invoke(meta), namespacedKey(key), type); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) { return null; }
    }
}
