package com.swag.infiniteblocks.listener;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class HandSwapListener implements Listener {

    private final InfiniteBlocksPlugin plugin;

    public HandSwapListener(InfiniteBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        boolean mainIsInfinite = plugin.getInfiniteBlockManager().isInfiniteBlock(event.getMainHandItem());
        boolean offIsInfinite = plugin.getInfiniteBlockManager().isInfiniteBlock(event.getOffHandItem());

        // Both hands having infinite blocks simultaneously has no safe resolution
        if (mainIsInfinite && offIsInfinite) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c[InfiniteBlocks] Cannot have infinite blocks in both hands!");
        }
    }
}
