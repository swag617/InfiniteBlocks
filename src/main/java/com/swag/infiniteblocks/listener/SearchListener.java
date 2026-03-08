package com.swag.infiniteblocks.listener;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import com.swag.infiniteblocks.gui.InfiniteBlocksGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SearchListener implements Listener {

    private final InfiniteBlocksPlugin plugin;
    private static final Map<UUID, Block> activeSearchSigns = new HashMap<>();

    public SearchListener(InfiniteBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    public static void addSearcher(UUID uuid, Block signBlock) {
        activeSearchSigns.put(uuid, signBlock);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block signBlock = activeSearchSigns.remove(player.getUniqueId());
        if (signBlock == null) return;

        // ✅ Remove the sign immediately
        event.setCancelled(true);
        signBlock.setType(Material.AIR);

        String query = event.getLine(0);
        if (query == null || query.isBlank() || query.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.RED + "Search cancelled.");
            Bukkit.getScheduler().runTask(plugin, () -> new InfiniteBlocksGUI(plugin, player, null).open());
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> new InfiniteBlocksGUI(plugin, player, query).open());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Block signBlock = activeSearchSigns.remove(event.getPlayer().getUniqueId());
        if (signBlock != null) {
            // ✅ Remove sign if they hit ESCAPE
            Bukkit.getScheduler().runTask(plugin, () -> signBlock.setType(Material.AIR));
        }
    }
}