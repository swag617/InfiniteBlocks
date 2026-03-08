package com.swag.infiniteblocks.listener;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import com.swag.infiniteblocks.gui.SchemeBlocksGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddBlockListener implements Listener {

    private final InfiniteBlocksPlugin plugin;
    private static final Map<UUID, String> addingPlayers = new HashMap<>();
    // ✅ NEW: Track the sign block so we can remove it
    private static final Map<UUID, Block> activeAddSigns = new HashMap<>();

    public AddBlockListener(InfiniteBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    // ✅ UPDATED: Now accepts the block reference
    public static void addAdder(UUID uuid, String scheme, Block signBlock) {
        addingPlayers.put(uuid, scheme);
        activeAddSigns.put(uuid, signBlock);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!addingPlayers.containsKey(player.getUniqueId())) return;

        String scheme = addingPlayers.remove(player.getUniqueId());
        // ✅ NEW: Get the sign block and remove it from tracking
        Block signBlock = activeAddSigns.remove(player.getUniqueId());

        // Read the first line of the sign
        String input = event.getLine(0);

        // ✅ NEW: Always cancel the event and remove the sign
        event.setCancelled(true);
        if (signBlock != null) {
            signBlock.setType(Material.AIR);
        }

        if (input == null || input.isEmpty() || input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.RED + "Addition cancelled.");
            Bukkit.getScheduler().runTask(plugin, () -> new SchemeBlocksGUI(plugin, player, scheme).open());
            return;
        }

        try {
            Material material = Material.valueOf(input.toUpperCase().replace(" ", "_"));
            if (!material.isBlock()) {
                player.sendMessage(ChatColor.RED + "That is not a valid block!");
            } else if (!material.isItem()) {
                player.sendMessage(ChatColor.RED + "That block doesn't have an item form!");
            } else {
                plugin.getConfigManager().addBlockToScheme(scheme, material.name());
                player.sendMessage(ChatColor.GREEN + "Added " + formatMaterialName(material) + " to " + formatSchemeName(scheme) + "!");
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid material name!");
        }

        // Re-open the GUI on the next tick
        Bukkit.getScheduler().runTask(plugin, () -> new SchemeBlocksGUI(plugin, player, scheme).open());
    }

    // ✅ NEW: Handle when player hits ESCAPE
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        addingPlayers.remove(player.getUniqueId());
        Block signBlock = activeAddSigns.remove(player.getUniqueId());

        if (signBlock != null) {
            // ✅ Remove sign if they hit ESCAPE (SignChangeEvent never fires)
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (signBlock.getType().name().contains("SIGN")) {
                    signBlock.setType(Material.AIR);
                }
            });
        }
    }

    private static String formatSchemeName(String scheme) {
        String[] parts = scheme.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static String formatMaterialName(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}