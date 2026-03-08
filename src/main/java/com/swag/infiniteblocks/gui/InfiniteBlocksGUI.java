package com.swag.infiniteblocks.gui;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class InfiniteBlocksGUI {

    private final InfiniteBlocksPlugin plugin;
    private final Player player;
    private final String searchQuery;
    private int currentPage;
    private Inventory inventory;
    private List<Material> blocks;
    private static final int ITEMS_PER_PAGE = 45;

    private static final Map<UUID, InfiniteBlocksGUI> activeGUIs = new HashMap<>();
    private static final Set<UUID> searchingPlayers = new HashSet<>();

    public InfiniteBlocksGUI(InfiniteBlocksPlugin plugin, Player player, String searchQuery) {
        this.plugin = plugin;
        this.player = player;
        this.searchQuery = searchQuery;
        this.currentPage = 0;

        if (searchQuery == null || searchQuery.isEmpty()) {
            this.blocks = plugin.getInfiniteBlockManager().getAllPlaceableBlocks();
        } else {
            this.blocks = plugin.getInfiniteBlockManager().searchBlocks(searchQuery);
        }
    }

    public void open() {
        createInventory();
        player.openInventory(inventory);
        // CRITICAL FIX: Register the GUI instance so GUIListener blocks item pickup
        activeGUIs.put(player.getUniqueId(), this);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public static InfiniteBlocksGUI getActiveGUI(Player player) {
        return activeGUIs.get(player.getUniqueId());
    }

    public static void removeActiveGUI(Player player) {
        activeGUIs.remove(player.getUniqueId());
    }

    public static void startSearching(Player player) { searchingPlayers.add(player.getUniqueId()); }
    public static boolean isSearching(Player player) { return searchingPlayers.contains(player.getUniqueId()); }
    public static void stopSearching(Player player) { searchingPlayers.remove(player.getUniqueId()); }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            return;
        }

        if (slot == 45 && currentPage > 0) {
            currentPage--;
            open();
            return;
        }

        if (slot == 53 && (currentPage + 1) * ITEMS_PER_PAGE < blocks.size()) {
            currentPage++;
            open();
            return;
        }

        if (slot == 49) {
            clicker.closeInventory();
            startSearching(clicker);
            clicker.sendMessage("");
            clicker.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "SEARCH MODE");
            clicker.sendMessage(ChatColor.GRAY + "Type your search query in chat.");
            clicker.sendMessage(ChatColor.RED + "Type 'cancel' to exit search.");
            clicker.sendMessage("");
            return;
        }

        if (slot == 50) {
            clicker.closeInventory();
            return;
        }

        if (slot < 45) {
            int index = currentPage * ITEMS_PER_PAGE + slot;
            if (index < blocks.size()) {
                Material material = blocks.get(index);

                // FIX: If we are in a specific scheme's block list, we should use THAT scheme
                // For now, we'll use the one assigned in config or the default
                String scheme = plugin.getConfigManager().getSchemeForBlock(material.name());
                ItemStack infiniteBlock = plugin.getInfiniteBlockManager().createInfiniteBlock(material, scheme);

                if (infiniteBlock == null) return;

                clicker.getInventory().addItem(infiniteBlock);
                clicker.sendMessage(ChatColor.GREEN + "Received infinite " + formatMaterialName(material) + "!");

                Bukkit.getScheduler().runTask(plugin, clicker::updateInventory);
            }
        }
    }

    private void createInventory() {
        String title = searchQuery == null ? "Infinite Blocks" : "Search: " + searchQuery;
        inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + title);

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, blocks.size());

        for (int i = startIndex; i < endIndex; i++) {
            Material material = blocks.get(i);
            ItemStack displayItem = new ItemStack(material);
            ItemMeta meta = displayItem.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + formatMaterialName(material));
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Click to receive!"));
                displayItem.setItemMeta(meta);
            }
            inventory.setItem(i - startIndex, displayItem);
        }

        if (currentPage > 0) inventory.setItem(45, createItem(Material.ARROW, ChatColor.YELLOW + "Previous Page"));
        if (endIndex < blocks.size()) inventory.setItem(53, createItem(Material.ARROW, ChatColor.YELLOW + "Next Page"));
        inventory.setItem(49, createItem(Material.COMPASS, ChatColor.AQUA + "" + ChatColor.BOLD + "Search"));
        inventory.setItem(50, createItem(Material.BARRIER, ChatColor.RED + "Close"));
    }

    private ItemStack createItem(Material m, String name) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private String formatMaterialName(Material material) {
        String[] words = material.name().split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return formatted.toString();
    }
}