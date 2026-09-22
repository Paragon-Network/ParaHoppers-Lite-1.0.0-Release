package network.paragon.hoppers.compat.item;

import network.paragon.hoppers.ParaHoppersLite;

/** Selects the strongest item tag backend available without hard-linking modern APIs. */
public final class ItemTagService {
    public static final String HOPPER_TYPE = "hopper_type";
    public static final String HOPPER_LEVEL = "hopper_level";
    private final ItemTagAdapter adapter;

    public ItemTagService(ParaHoppersLite plugin) {
        ItemTagAdapter selected;
        try {
            selected = new ReflectivePdcItemTagAdapter(plugin);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            selected = new LegacyLoreItemTagAdapter();
        }
        adapter = selected;
        plugin.getLogger().info("Item metadata compatibility backend: " + adapter.name());
    }
    public ItemTagAdapter adapter() { return adapter; }
}
