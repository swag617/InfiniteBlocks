package com.swag.infiniteblocks.listener;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import com.swag.infiniteblocks.gui.SchemeBlocksGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SchemeBlocksGUIListener implements Listener {

    private final InfiniteBlocksPlugin plugin;

    public SchemeBlocksGUIListener(InfiniteBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Theme Management")) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;

        // Extract the scheme key from the lore we set in SchemeBlocksGUI
        if (event.getCurrentItem().getItemMeta().hasLore()) {
            String firstLine = event.getCurrentItem().getItemMeta().getLore().get(0);
            String schemeKey = ChatColor.stripColor(firstLine).replace("Theme Key: ", "");

            player.sendMessage("§aSelected theme: §f" + schemeKey);
            // Here you could open a sub-menu to add/remove blocks from this specific scheme
            player.closeInventory();
        }
    }
}