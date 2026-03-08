package com.swag.infiniteblocks.command;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import com.swag.infiniteblocks.gui.ConfigGUI;
import com.swag.infiniteblocks.gui.InfiniteBlocksGUI;
import com.swag.infiniteblocks.manager.SchemeCreationManager;
import com.swag.infiniteblocks.utils.BirdflopParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InfBlocksCommand implements CommandExecutor, TabCompleter {

    private final InfiniteBlocksPlugin plugin;

    public InfBlocksCommand(InfiniteBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player) && (args.length == 0 || !args[0].equalsIgnoreCase("get"))) {
            sender.sendMessage(ChatColor.RED + "This command requires a player.");
            return true;
        }

        Player player = sender instanceof Player ? (Player) sender : null;

        if (args.length == 0) {
            if (player != null) {
                player.sendMessage(ChatColor.YELLOW + "InfiniteBlocks commands:");
                player.sendMessage(ChatColor.AQUA + "/ib get" + ChatColor.WHITE + " - open block selector GUI");
                player.sendMessage(ChatColor.AQUA + "/ib get <player> <block> <amount>" + ChatColor.WHITE + " - give infinite blocks");
                player.sendMessage(ChatColor.AQUA + "/ib scheme" + ChatColor.WHITE + " - open scheme config GUI");
                player.sendMessage(ChatColor.AQUA + "/ib scheme create <displayName>" + ChatColor.WHITE + " - create a new scheme");
                player.sendMessage(ChatColor.AQUA + "/ib scheme remove <scheme>" + ChatColor.WHITE + " - remove a scheme");
                player.sendMessage(ChatColor.AQUA + "/ib scheme addblock <scheme> [material|container]" + ChatColor.WHITE + " - add blocks to scheme");
                player.sendMessage(ChatColor.AQUA + "/ib scheme removeblock <scheme> [material|container]" + ChatColor.WHITE + " - remove blocks from scheme");
                player.sendMessage(ChatColor.AQUA + "/ib reload" + ChatColor.WHITE + " - reload plugin config");
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("get")) {
            if (args.length == 1) {
                if (player == null) {
                    sender.sendMessage(ChatColor.RED + "Console cannot open GUIs.");
                    return true;
                }
                new InfiniteBlocksGUI(plugin, player, null).open();
                return true;
            }

            if (args.length == 4) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
                    return true;
                }

                Material material;
                try {
                    material = Material.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid block: " + args[2]);
                    return true;
                }

                if (!material.isBlock() || !material.isItem()) {
                    sender.sendMessage(ChatColor.RED + "That material cannot be made infinite.");
                    return true;
                }

                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                    if (amount <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
                    return true;
                }

                String scheme = plugin.getConfigManager().getSchemeForBlock(material.name());
                ItemStack item = plugin.getInfiniteBlockManager().createInfiniteBlock(material, scheme);
                if (item == null) {
                    sender.sendMessage(ChatColor.RED + "Failed to create infinite block for " + material.name());
                    return true;
                }

                item.setAmount(Math.min(amount, 64));
                target.getInventory().addItem(item);

                sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " infinite " + material.name() + " to " + target.getName());
                target.sendMessage(ChatColor.GREEN + "You received " + amount + " infinite " + material.name() + "!");
                return true;
            }

            sender.sendMessage(ChatColor.RED + "Usage: /ib get OR /ib get <player> <block> <amount>");
            return true;
        }

        if (sub.equals("scheme")) {
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Console cannot use scheme commands.");
                return true;
            }

            if (args.length == 1) {
                new ConfigGUI(plugin, player).open();
                return true;
            }

            String action = args[1].toLowerCase();

            if (action.equals("remove")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /ib scheme remove <scheme>");
                    return true;
                }
                String schemeKey = args[2].toLowerCase();
                if (!plugin.getConfigManager().getSchemes().contains(schemeKey)) {
                    player.sendMessage(ChatColor.RED + "Scheme '" + schemeKey + "' does not exist.");
                    return true;
                }
                if (schemeKey.equals(plugin.getConfigManager().getDefaultScheme())) {
                    player.sendMessage(ChatColor.RED + "Cannot remove the default scheme.");
                    return true;
                }
                try {
                    plugin.getConfigManager().removeScheme(schemeKey);
                    player.sendMessage(ChatColor.GREEN + "Scheme '" + schemeKey + "' removed successfully.");
                } catch (IOException e) {
                    player.sendMessage(ChatColor.RED + "Failed to remove scheme.");
                    e.printStackTrace();
                }
                return true;
            }

            if (action.equals("create")) {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /ib scheme create <displayName>");
                    return true;
                }
                String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', displayName));
                String key = stripped.toLowerCase().replaceAll("[^a-z0-9]+", "_");

                if (plugin.getConfigManager().getSchemes().contains(key)) {
                    player.sendMessage(ChatColor.RED + "A scheme with key '" + key + "' already exists.");
                    return true;
                }

                List<String> colors = BirdflopParser.parseHexList(displayName);
                if (colors.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Could not detect any colors. Use Birdflop format.");
                    return true;
                }

                SchemeCreationManager scm = plugin.getSchemeCreationManager();
                scm.start(key, player.getUniqueId());
                SchemeCreationManager.Session session = scm.get(player.getUniqueId());
                if (session != null) {
                    session.displayName = displayName;
                    session.birdflopRaw = displayName;
                    session.step = SchemeCreationManager.Step.LORE;
                }

                player.sendMessage(ChatColor.GREEN + "Creating scheme: " + ChatColor.translateAlternateColorCodes('&', displayName));
                player.sendMessage(ChatColor.YELLOW + "Type lore lines in chat.");
                return true;
            }

            if (action.equals("addblock")) {
                return handleAddBlock(player, args);
            }

            if (action.equals("removeblock")) {
                return handleRemoveBlock(player, args);
            }

            player.sendMessage(ChatColor.RED + "Usage: /ib scheme [create|remove|addblock|removeblock]");
            return true;
        }

        if (sub.equals("reload")) {
            plugin.getConfigManager().reload();
            sender.sendMessage(ChatColor.GREEN + "InfiniteBlocks configuration reloaded.");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /ib for help.");
        return true;
    }

    private boolean handleAddBlock(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /ib scheme addblock <scheme> [material|container]");
            return true;
        }

        String scheme = args[2].toLowerCase();
        if (!plugin.getConfigManager().getSchemes().contains(scheme)) {
            player.sendMessage(ChatColor.RED + "Scheme '" + scheme + "' does not exist.");
            return true;
        }

        if (args.length == 4 && args[3].equalsIgnoreCase("container")) {
            Block target = player.getTargetBlockExact(5);
            if (target == null || !(target.getState() instanceof Container)) {
                player.sendMessage(ChatColor.RED + "You must be looking at a container.");
                return true;
            }
            Container container = (Container) target.getState();
            int count = 0;
            for (ItemStack item : container.getInventory().getContents()) {
                if (item != null && item.getType().isBlock() && item.getType().isItem()) {
                    plugin.getConfigManager().addBlockToScheme(scheme, item.getType().name());
                    count++;
                }
            }
            player.sendMessage(ChatColor.GREEN + "Added " + count + " blocks from container to scheme '" + scheme + "'.");
            return true;
        }

        Material mat;
        if (args.length == 4) {
            try {
                mat = Material.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid material: " + args[3]);
                return true;
            }
        } else {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "Hold an item or specify a material.");
                return true;
            }
            mat = hand.getType();
        }

        if (!mat.isBlock() || !mat.isItem()) {
            player.sendMessage(ChatColor.RED + mat.name() + " is not a valid block.");
            return true;
        }

        plugin.getConfigManager().addBlockToScheme(scheme, mat.name());
        player.sendMessage(ChatColor.GREEN + "Added " + mat.name() + " to scheme '" + scheme + "'.");
        return true;
    }

    private boolean handleRemoveBlock(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /ib scheme removeblock <scheme> [material|container]");
            return true;
        }

        String scheme = args[2].toLowerCase();
        if (!plugin.getConfigManager().getSchemes().contains(scheme)) {
            player.sendMessage(ChatColor.RED + "Scheme '" + scheme + "' does not exist.");
            return true;
        }

        if (args.length == 4 && args[3].equalsIgnoreCase("container")) {
            Block target = player.getTargetBlockExact(5);
            if (target == null || !(target.getState() instanceof Container)) {
                player.sendMessage(ChatColor.RED + "You must be looking at a container.");
                return true;
            }
            Container container = (Container) target.getState();
            int count = 0;
            for (ItemStack item : container.getInventory().getContents()) {
                if (item != null) {
                    plugin.getConfigManager().removeBlockFromScheme(scheme, item.getType().name());
                    count++;
                }
            }
            player.sendMessage(ChatColor.GREEN + "Removed " + count + " blocks from scheme '" + scheme + "'.");
            return true;
        }

        Material mat;
        if (args.length == 4) {
            try {
                mat = Material.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid material: " + args[3]);
                return true;
            }
        } else {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "Hold an item or specify a material.");
                return true;
            }
            mat = hand.getType();
        }

        plugin.getConfigManager().removeBlockFromScheme(scheme, mat.name());
        player.sendMessage(ChatColor.GREEN + "Removed " + mat.name() + " from scheme '" + scheme + "'.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("get", "scheme", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args[0].equalsIgnoreCase("get")) {
            if (args.length == 2) return null; // Players
            if (args.length == 3) return Arrays.stream(Material.values()).filter(m -> m.isBlock() && m.isItem()).map(Enum::name)
                    .filter(n -> n.startsWith(args[2].toUpperCase())).collect(Collectors.toList());
            if (args.length == 4) return Arrays.asList("1", "16", "32", "64");
        }

        if (args[0].equalsIgnoreCase("scheme")) {
            if (args.length == 2) return Arrays.asList("create", "remove", "addblock", "removeblock").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            if (args.length == 3) return new ArrayList<>(plugin.getConfigManager().getSchemes()).stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            if (args.length == 4 && (args[1].equalsIgnoreCase("addblock") || args[1].equalsIgnoreCase("removeblock"))) {
                List<String> options = new ArrayList<>();
                options.add("container");
                options.addAll(Arrays.stream(Material.values()).filter(m -> m.isBlock() && m.isItem()).map(Enum::name).collect(Collectors.toList()));
                return options.stream().filter(s -> s.startsWith(args[3].toUpperCase())).collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}