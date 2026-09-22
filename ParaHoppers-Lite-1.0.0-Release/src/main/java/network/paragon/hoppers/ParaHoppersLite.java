package network.paragon.hoppers;

import network.paragon.hoppers.command.ParaHoppersCommand;
import network.paragon.hoppers.compat.CompatibilityManager;
import network.paragon.hoppers.compat.item.ItemTagService;
import network.paragon.hoppers.config.Messages;
import network.paragon.hoppers.hopper.HopperItemFactory;
import network.paragon.hoppers.hopper.HopperRegistry;
import network.paragon.hoppers.listener.HopperListener;
import network.paragon.hoppers.service.CollectionEngine;
import network.paragon.hoppers.service.LinkManager;
import network.paragon.hoppers.service.TeleportEngine;
import network.paragon.hoppers.service.MiningStateManager;
import network.paragon.hoppers.service.MiningEngine;
import network.paragon.hoppers.service.QuarryStateManager;
import network.paragon.hoppers.service.FilterManager;
import network.paragon.hoppers.service.PerformanceMonitor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParaHoppersLite extends JavaPlugin {
    private HopperRegistry hopperRegistry;
    private HopperItemFactory hopperItemFactory;
    private Messages messages;
    private ItemTagService itemTagService;
    private CollectionEngine collectionEngine;
    private LinkManager linkManager;
    private TeleportEngine teleportEngine;
    private MiningStateManager miningStateManager;
    private MiningEngine miningEngine;
    private QuarryStateManager quarryStateManager;
    private FilterManager filterManager;
    private PerformanceMonitor performanceMonitor;
    private CompatibilityManager compatibility;
    private final network.paragon.hoppers.compat.VisualCompat visuals = new network.paragon.hoppers.compat.VisualCompat();

    @Override
    public void onEnable() {
        if (!network.paragon.hoppers.compat.BuildFlavor.check(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        compatibility = new CompatibilityManager();
        getLogger().info("Minecraft " + compatibility.version().normalized() + " detected; compatibility family: " + compatibility.family());

        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("menus/hopper-menu.yml", true);

        itemTagService = new ItemTagService(this);

        messages = new Messages(this);
        hopperRegistry = new HopperRegistry(this);
        hopperRegistry.load();
        hopperItemFactory = new HopperItemFactory(this);

        ParaHoppersCommand command = new ParaHoppersCommand(this);
        PluginCommand pluginCommand = getCommand("parahoppers");
        if (pluginCommand == null) {
            throw new IllegalStateException("Command 'parahoppers' is missing from plugin.yml");
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new HopperListener(this, command), this);
        miningStateManager = new MiningStateManager(this);
        quarryStateManager = new QuarryStateManager(this);
        filterManager = new FilterManager(this);
        performanceMonitor = new PerformanceMonitor(this);
        miningEngine = new MiningEngine(this, miningStateManager, quarryStateManager);
        miningEngine.start();
        linkManager = new LinkManager(this);
        teleportEngine = new TeleportEngine(this, linkManager);
        teleportEngine.start();
        collectionEngine = new CollectionEngine(this);
        collectionEngine.start();

        getLogger().info("Para Hoppers [Lite] 1.0.0 enabled with "
                + hopperRegistry.size() + " registered hopper(s).");
    }

    @Override
    public void onDisable() {
        if (collectionEngine != null) collectionEngine.stop();
        if (teleportEngine != null) teleportEngine.stop();
        if (miningEngine != null) miningEngine.stop();
        if (hopperRegistry != null) {
            hopperRegistry.save();
        }
    }

    public void reloadPluginFiles() {
        reloadConfig();
        messages.reload();
        if (collectionEngine != null) collectionEngine.restart();
        if (teleportEngine != null) teleportEngine.restart();
        if (miningEngine != null) miningEngine.restart();
    }

    public HopperRegistry getHopperRegistry() { return hopperRegistry; }
    public HopperItemFactory getHopperItemFactory() { return hopperItemFactory; }
    public Messages getMessages() { return messages; }
    public ItemTagService getItemTagService() { return itemTagService; }
    public LinkManager getLinkManager() { return linkManager; }
    public MiningStateManager getMiningStateManager() { return miningStateManager; }
    public QuarryStateManager getQuarryStateManager() { return quarryStateManager; }
    public FilterManager getFilterManager() { return filterManager; }
    public PerformanceMonitor getPerformanceMonitor() { return performanceMonitor; }
    public network.paragon.hoppers.compat.VisualCompat getVisuals() { return visuals; }
    public CompatibilityManager getCompatibility() { return compatibility; }
}

