package com.swag.infiniteblocks.listener;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class BlockPlaceListener implements Listener {

    private final InfiniteBlocksPlugin plugin;

    public BlockPlaceListener(InfiniteBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemInHand();

        if (itemInHand == null) return;
        if (!plugin.getInfiniteBlockManager().isInfiniteBlock(itemInHand)) return;

        EquipmentSlot hand = event.getHand();
        Material blockType = itemInHand.getType();

        // Sanity check: both hands should never hold infinite blocks simultaneously
        ItemStack otherHandItem = (hand == EquipmentSlot.HAND)
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();

        if (plugin.getInfiniteBlockManager().isInfiniteBlock(otherHandItem)) {
            plugin.getLogger().warning("[InfiniteBlocks] " + player.getName() + " had infinite blocks in both hands");
            event.setCancelled(true);
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            } else {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
            player.sendMessage("§c[InfiniteBlocks] Error: Cannot have infinite blocks in both hands!");
            return;
        }

        // Auto-update the item to whatever scheme is currently assigned to this block
        String correctScheme = plugin.getConfigManager().getSchemeForBlock(blockType.name());
        String currentScheme = detectCurrentScheme(itemInHand);
        if (!correctScheme.equals(currentScheme)) {
            plugin.getLogger().info("[InfiniteBlocks] Updating " + player.getName() + "'s "
                    + blockType.name() + " scheme: " + currentScheme + " -> " + correctScheme);
        }

        ItemStack infiniteBlock = plugin.getInfiniteBlockManager().createInfiniteBlock(blockType, correctScheme);
        if (infiniteBlock == null) return;

        // Immediate restore — runs synchronously before vanilla decrements the item
        if (hand == EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(infiniteBlock);
        } else if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(infiniteBlock);
        }

        // Record the exact slot so delayed restores target it directly.
        // getItemInMainHand/setItemInMainHand follow the currently selected slot,
        // so using those in delayed tasks can write to the wrong slot if the player
        // scrolls or presses F between now and when the task fires.
        final int placementSlot = (hand == EquipmentSlot.HAND)
                ? player.getInventory().getHeldItemSlot() : -1;

        // Mushroom blocks: vanilla consumes the item one tick after placement
        if (isMushroom(blockType)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                ItemStack slotItem = (hand == EquipmentSlot.HAND)
                        ? player.getInventory().getItem(placementSlot)
                        : player.getInventory().getItemInOffHand();

                if (slotItem == null || slotItem.getType() == Material.AIR || slotItem.getAmount() == 0) {
                    if (hand == EquipmentSlot.HAND) {
                        player.getInventory().setItem(placementSlot, infiniteBlock);
                    } else {
                        player.getInventory().setItemInOffHand(infiniteBlock);
                    }
                }
                player.updateInventory();
            });
        } else {
            if (hand == EquipmentSlot.OFF_HAND) {
                Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            }
        }

        // Safety-net restore after vanilla's item-consumption tick
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // If the other hand holds an infinite block, restoring here would duplicate it
            ItemStack otherHand = (hand == EquipmentSlot.HAND)
                    ? player.getInventory().getItemInOffHand()
                    : player.getInventory().getItemInMainHand();

            if (plugin.getInfiniteBlockManager().isInfiniteBlock(otherHand)) {
                plugin.getLogger().warning("[InfiniteBlocks] Skipped restore for " + player.getName()
                        + " — infinite block in other hand");
                return;
            }

            ItemStack slotItem = (hand == EquipmentSlot.HAND)
                    ? player.getInventory().getItem(placementSlot)
                    : player.getInventory().getItemInOffHand();

            // Only restore if the slot is empty; if something else is there, leave it alone
            if (slotItem == null || slotItem.getType() == Material.AIR || slotItem.getAmount() == 0) {
                if (hand == EquipmentSlot.HAND) {
                    player.getInventory().setItem(placementSlot, infiniteBlock);
                } else {
                    player.getInventory().setItemInOffHand(infiniteBlock);
                }
                player.updateInventory();
            }
        }, 2L);
    }

    private String detectCurrentScheme(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return plugin.getConfigManager().getDefaultScheme();
        }

        String displayName = item.getItemMeta().getDisplayName();
        java.util.List<String> extractedColors = extractHexColors(displayName);

        if (extractedColors.isEmpty()) {
            return plugin.getConfigManager().getDefaultScheme();
        }

        // Match the first hex color in the display name against known scheme gradients
        for (String schemeName : plugin.getConfigManager().getSchemes()) {
            java.util.List<String> schemeColors = plugin.getConfigManager().getNameGradient(schemeName);
            if (schemeColors == null || schemeColors.isEmpty()) continue;

            String extractedFirst = extractedColors.get(0).toUpperCase().replace("#", "");
            String schemeFirst = schemeColors.get(0).toUpperCase().replace("#", "");

            if (extractedFirst.equals(schemeFirst)) {
                return schemeName;
            }
        }

        return plugin.getConfigManager().getDefaultScheme();
    }

    private java.util.List<String> extractHexColors(String text) {
        java.util.List<String> colors = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) return colors;

        // Matches Minecraft hex color format: §x§r§r§g§g§b§b
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("§x(§[0-9A-Fa-f]){6}");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String match = matcher.group();
            String hex = match.replaceAll("[§x]", "");
            if (hex.length() == 6) {
                colors.add("#" + hex.toUpperCase());
            }
        }

        return colors;
    }

    private String formatSchemeName(String scheme) {
        if (scheme == null) return "Unknown";
        String[] parts = scheme.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            if (part.isEmpty()) continue;
            sb.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private boolean isMushroom(Material material) {
        return material == Material.BROWN_MUSHROOM_BLOCK
                || material == Material.RED_MUSHROOM_BLOCK
                || material == Material.MUSHROOM_STEM;
    }
}
