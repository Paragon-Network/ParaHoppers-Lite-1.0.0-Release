package network.paragon.hoppers.hopper;

import network.paragon.hoppers.ParaHoppersLite;
import network.paragon.hoppers.compat.item.ItemTagAdapter;
import network.paragon.hoppers.compat.item.ItemTagService;
import network.paragon.hoppers.config.Messages;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

public final class HopperItemFactory {
    private final ParaHoppersLite plugin;
    private final ItemTagAdapter tags;

    public HopperItemFactory(ParaHoppersLite plugin) {
        this.plugin = plugin;
        this.tags = plugin.getItemTagService().adapter();
    }

    public ItemStack create(HopperType type, int level, int amount) {
        ItemStack item = new ItemStack(Material.HOPPER, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(Messages.color(plugin.getConfig().getString(
                "hopper.display-name", "&d&lPara Hopper &8[&fLite&8]")));

        List<String> lore = new ArrayList<String>();
        lore.add(Messages.color("&7Type: &f" + type.name()));
        lore.add(Messages.color("&7Level: &f" + level));
        lore.add("");
        lore.add(Messages.color("&8Para Hoppers [Lite]"));
        meta.setLore(lore);

        tags.setString(meta, ItemTagService.HOPPER_TYPE, type.name());
        tags.setInt(meta, ItemTagService.HOPPER_LEVEL, level);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isParaHopper(ItemStack item) {
        if (item == null || item.getType() != Material.HOPPER || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && tags.getString(meta, ItemTagService.HOPPER_TYPE) != null;
    }

    public HopperType getType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return HopperType.COLLECTOR;
        String value = tags.getString(item.getItemMeta(), ItemTagService.HOPPER_TYPE);
        if (value == null) return HopperType.COLLECTOR;
        try { return HopperType.valueOf(value); }
        catch (IllegalArgumentException ignored) { return HopperType.COLLECTOR; }
    }

    public int getLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 1;
        Integer level = tags.getInt(item.getItemMeta(), ItemTagService.HOPPER_LEVEL);
        return level == null || level.intValue() < 1 ? 1 : level.intValue();
    }
}
