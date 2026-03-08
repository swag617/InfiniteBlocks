package com.swag.infiniteblocks.listener;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import com.swag.infiniteblocks.gui.ConfigGUI;
import com.swag.infiniteblocks.gui.InfiniteBlocksGUI;
import com.swag.infiniteblocks.gui.SchemeBlocksGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GUIListener implements Listener {

    private final InfiniteBlocksPlugin plugin;

    public GUIListener(InfiniteBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        InfiniteBlocksGUI ib = InfiniteBlocksGUI.getActiveGUI(player);
        if (ib != null && event.getInventory().equals(ib.getInventory())) {
            ib.handleClick(event);
            return;
        }

        SchemeBlocksGUI sg = SchemeBlocksGUI.getActiveGUI(player);
        if (sg != null && event.getInventory().equals(sg.getInventory())) {
            sg.handleClick(event);
            return;
        }

        ConfigGUI cg = ConfigGUI.getActiveGUI(player);
        if (cg != null && event.getInventory().equals(cg.getInventory())) {
            cg.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        InfiniteBlocksGUI.removeActiveGUI(player);
        SchemeBlocksGUI.removeActiveGUI(player);
        ConfigGUI.removeActiveGUI(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        InfiniteBlocksGUI.removeActiveGUI(player);
        InfiniteBlocksGUI.stopSearching(player);
        SchemeBlocksGUI.removeActiveGUI(player);
        ConfigGUI.removeActiveGUI(player);
        plugin.getSchemeCreationManager().end(player.getUniqueId());
    }
}