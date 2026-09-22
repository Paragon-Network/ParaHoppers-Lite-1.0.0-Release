package network.paragon.hoppers.command;

import network.paragon.hoppers.ParaHoppersLite;
import network.paragon.hoppers.hopper.HopperType;
import network.paragon.hoppers.hopper.ParagonHopper;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class ParaHoppersCommand implements CommandExecutor, TabCompleter {
    private final ParaHoppersLite plugin;
    private final Set<UUID> inspectMode = new HashSet<>();
    private final Map<UUID, String> linkSources = new HashMap<>();

    public ParaHoppersCommand(ParaHoppersLite plugin) {
        this.plugin = plugin;
    }

    public boolean isInspecting(UUID playerId) {
        return inspectMode.contains(playerId);
    }

    public void disableInspect(UUID playerId) { inspectMode.remove(playerId); }
    public void beginLink(UUID playerId, String sourceKey) { linkSources.put(playerId, sourceKey); }
    public String getLinkSource(UUID playerId) { return linkSources.get(playerId); }
    public void clearLink(UUID playerId) { linkSources.remove(playerId); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§d§lPara Hoppers §8[§fLite§8]");
            sender.sendMessage("§f/" + label + " inspect §7- inspect a placed Para Hopper");
            sender.sendMessage("§f/" + label + " link §7- look at a Para Hopper, then choose its destination");
            sender.sendMessage("§f/" + label + " unlink §7- remove the looked-at hopper link");
            if (sender.hasPermission("parahoppers.admin.give"))
                sender.sendMessage("§f/" + label + " give <player> <collector> [amount]");
            if (sender.hasPermission("parahoppers.admin.list"))
                sender.sendMessage("§f/" + label + " list §7- list registered hoppers");
            if (sender.hasPermission("parahoppers.admin.reload"))
                sender.sendMessage("§f/" + label + " reload §7- reload configuration");
            if (sender.hasPermission("parahoppers.admin.stats"))
                sender.sendMessage("§f/" + label + " stats §7- view runtime statistics");
            return true;
        }

        if (args[0].equalsIgnoreCase("inspect")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessages().get("player-only"));
                return true;
            }
            Player player=(Player)sender;
            if (!player.hasPermission("parahoppers.inspect")) {
                player.sendMessage(plugin.getMessages().get("no-permission"));
                return true;
            }
            if (!inspectMode.add(player.getUniqueId())) {
                inspectMode.remove(player.getUniqueId());
                player.sendMessage(plugin.getMessages().get("inspect-disabled"));
            } else {
                player.sendMessage(plugin.getMessages().get("inspect-enabled"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("link")) {
            if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMessages().get("player-only")); return true; }
            Player player=(Player)sender;
            org.bukkit.block.Block b=network.paragon.hoppers.compat.RuntimeCompat.targetBlock(player,6);
            if(b==null||b.getType()!=org.bukkit.Material.HOPPER){player.sendMessage(plugin.getMessages().get("not-para-hopper"));return true;}
            ParagonHopper h=plugin.getHopperRegistry().get(b.getLocation());
            if(h==null){player.sendMessage(plugin.getMessages().get("not-para-hopper"));return true;}
            if(plugin.getConfig().getBoolean("teleport.owner-only-linking",true)&&!h.owner().equals(player.getUniqueId())&&!player.hasPermission("parahoppers.admin")){player.sendMessage(plugin.getMessages().get("link-owner"));return true;}
            beginLink(player.getUniqueId(),h.locationKey());player.sendMessage(plugin.getMessages().get("link-started"));return true;
        }
        if (args[0].equalsIgnoreCase("unlink")) {
            if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMessages().get("player-only")); return true; }
            Player player=(Player)sender;
            org.bukkit.block.Block b=network.paragon.hoppers.compat.RuntimeCompat.targetBlock(player,6);
            if(b==null||b.getType()!=org.bukkit.Material.HOPPER){player.sendMessage(plugin.getMessages().get("not-para-hopper"));return true;}
            ParagonHopper h=plugin.getHopperRegistry().get(b.getLocation());
            if(h==null){player.sendMessage(plugin.getMessages().get("not-para-hopper"));return true;}
            if(plugin.getLinkManager().remove(h))player.sendMessage(plugin.getMessages().get("link-removed"));else player.sendMessage(plugin.getMessages().get("link-none"));return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("parahoppers.admin.give")) {
                sender.sendMessage(plugin.getMessages().get("no-permission"));
                return true;
            }
            if (args.length < 3) return false;

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getMessages().get("player-not-found"));
                return true;
            }

            HopperType type;
            try {
                type = HopperType.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(plugin.getMessages().get("invalid-type"));
                return true;
            }

            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                    if (amount < 1) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    sender.sendMessage(plugin.getMessages().get("invalid-number"));
                    return true;
                }
            }

            int level = plugin.getConfig().getInt("hopper.default-level", 1);
            target.getInventory().addItem(plugin.getHopperItemFactory().create(type, level, amount));
            sender.sendMessage(plugin.getMessages().get("given")
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%type%", type.name())
                    .replace("%player%", target.getName()));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("parahoppers.admin.list")) {
                sender.sendMessage(plugin.getMessages().get("no-permission"));
                return true;
            }
            sender.sendMessage("§dRegistered Para Hoppers: §f" + plugin.getHopperRegistry().size());
            for (ParagonHopper hopper : plugin.getHopperRegistry().all()) {
                sender.sendMessage("§7- §f" + hopper.type() + " §7L" + hopper.level()
                        + " §8@ §f" + hopper.world() + " "
                        + hopper.x() + "," + hopper.y() + "," + hopper.z());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("parahoppers.admin.reload")) {
                sender.sendMessage(plugin.getMessages().get("no-permission"));
                return true;
            }
            plugin.reloadPluginFiles();
            sender.sendMessage(plugin.getMessages().get("reloaded"));
            return true;
        }

        if (args[0].equalsIgnoreCase("stats")) {
            if (!sender.hasPermission("parahoppers.admin.stats")) {
                sender.sendMessage(plugin.getMessages().get("no-permission"));
                return true;
            }
            network.paragon.hoppers.service.PerformanceMonitor stats = plugin.getPerformanceMonitor();
            sender.sendMessage("§d§lPara Hoppers §8[§fLite§8] §7Runtime Statistics");
            sender.sendMessage("§7TPS: §f" + String.format(Locale.ROOT, "%.2f", stats.tps()));
            sender.sendMessage("§7Registered Hoppers: §f" + stats.hopperCount());
            sender.sendMessage("§7Active Mining Hoppers: §f" + stats.activeMiners());
            sender.sendMessage("§7Blocks Mined: §f" + stats.minedBlocks());
            sender.sendMessage("§7Collection Cycles: §f" + stats.collectionCycles());
            sender.sendMessage("§7Mining Cycles: §f" + stats.miningCycles());
            sender.sendMessage("§7Teleport Cycles: §f" + stats.teleportCycles());
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> values = new ArrayList<>(java.util.Arrays.asList("help", "inspect", "link", "unlink"));
            if (sender.hasPermission("parahoppers.admin.give")) values.add("give");
            if (sender.hasPermission("parahoppers.admin.list")) values.add("list");
            if (sender.hasPermission("parahoppers.admin.reload")) values.add("reload");
            if (sender.hasPermission("parahoppers.admin.stats")) values.add("stats");
            return filter(values, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give"))
            return filter(network.paragon.hoppers.compat.RuntimeCompat.onlinePlayers().stream().map(Player::getName).collect(java.util.stream.Collectors.toList()), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("give"))
            return filter(Arrays.stream(HopperType.values()).map(t -> t.name().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toList()), args[2]);
        return java.util.Collections.emptyList();
    }

    private List<String> filter(Collection<String> values, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted().collect(Collectors.toList());
    }
}
