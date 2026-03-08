package com.swag.infiniteblocks.gui;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SchemeBlocksGUI {

    private final InfiniteBlocksPlugin plugin;
    private final Player player;
    private final String scheme;
    private Inventory inventory;
    private int currentPage;
    private List<String> blocks;
    private static final int ITEMS_PER_PAGE = 45;
    private static final Map<UUID, SchemeBlocksGUI> activeGUIs = new HashMap<>();

    public SchemeBlocksGUI(InfiniteBlocksPlugin plugin, Player player, String scheme) {
        this.plugin = plugin;
        this.player = player;
        this.scheme = scheme;
        this.currentPage = 0;
        this.blocks = plugin.getConfigManager().getSchemeBlocks(scheme);
    }

    public void open() {
        createInventory();
        player.openInventory(inventory);
        activeGUIs.put(player.getUniqueId(), this);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public static SchemeBlocksGUI getActiveGUI(Player player) {
        return activeGUIs.get(player.getUniqueId());
    }

    public static void removeActiveGUI(Player player) {
        activeGUIs.remove(player.getUniqueId());
    }

    public void handleClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(this.inventory)) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 45 && currentPage > 0) {
            currentPage--;
            open();
        } else if (slot == 53 && (currentPage + 1) * ITEMS_PER_PAGE < blocks.size()) {
            currentPage++;
            open();
        } else if (slot == 49) {
            clicker.closeInventory();
            Block block = clicker.getLocation().getBlock();
            block.setType(Material.OAK_SIGN, false);

            com.swag.infiniteblocks.listener.AddBlockListener.addAdder(clicker.getUniqueId(), scheme, block);

            clicker.openSign((org.bukkit.block.Sign) block.getState());
            return;
        } else if (slot == 48) {
            clicker.closeInventory();
            new ConfigGUI(plugin, clicker).open();
        } else if (slot < 45) {
            int index = currentPage * ITEMS_PER_PAGE + slot;
            if (index < blocks.size()) {
                String materialName = blocks.get(index);
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return;
                }

                if (event.isLeftClick()) {
                    ItemStack infiniteBlock = plugin.getInfiniteBlockManager().createInfiniteBlock(material, scheme);
                    if (infiniteBlock != null) {
                        infiniteBlock.setAmount(1);
                        clicker.getInventory().addItem(infiniteBlock);
                        clicker.sendMessage(ChatColor.GREEN + "You received an infinite " + formatMaterialName(material) + "!");
                    }
                } else if (event.isRightClick()) {
                    plugin.getConfigManager().removeBlockFromScheme(scheme, materialName);
                    blocks = plugin.getConfigManager().getSchemeBlocks(scheme);
                    open();
                    clicker.sendMessage(ChatColor.RED + formatMaterialName(material) + " removed from " + formatSchemeName(scheme) + "!");
                }
            }
        }
    }

    private void createInventory() {
        String schemeName = formatSchemeName(scheme);
        String title = ChatColor.DARK_PURPLE + "Scheme: " + ChatColor.LIGHT_PURPLE + schemeName;

        inventory = Bukkit.createInventory(null, 54, title);

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, blocks.size());

        for (int i = startIndex; i < endIndex; i++) {
            String materialName = blocks.get(i);
            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                continue;
            }

            ItemStack displayItem = new ItemStack(material);
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + formatMaterialName(material));
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "This block uses " + schemeName,
                        ChatColor.GREEN + "Left-click to get block",
                        ChatColor.RED + "Right-click to remove"
                ));
                displayItem.setItemMeta(meta);
            }
            inventory.setItem(i - startIndex, displayItem);
        }

        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta m = prev.getItemMeta();
            m.setDisplayName(ChatColor.YELLOW + "Previous Page");
            prev.setItemMeta(m);
            inventory.setItem(45, prev);
        }
        if (endIndex < blocks.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta m = next.getItemMeta();
            m.setDisplayName(ChatColor.YELLOW + "Next Page");
            next.setItemMeta(m);
            inventory.setItem(53, next);
        }

        ItemStack add = new ItemStack(Material.EMERALD);
        ItemMeta addM = add.getItemMeta();
        addM.setDisplayName(ChatColor.GREEN + "Add Block to Config");
        add.setItemMeta(addM);
        inventory.setItem(49, add);

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backM = back.getItemMeta();
        backM.setDisplayName(ChatColor.RED + "Back");
        back.setItemMeta(backM);
        inventory.setItem(48, back);

        // --- PREVIEW ITEM ---
        ItemStack preview = plugin.getInfiniteBlockManager().createInfiniteBlock(Material.BEDROCK, scheme);
        if (preview != null) {
            ItemMeta previewMeta = preview.getItemMeta();
            if (previewMeta != null) {
                List<String> lore = previewMeta.getLore();
                if (lore == null) lore = new ArrayList<>();
                lore.add(0, "§6§lPREVIEW");
                lore.add(1, "§7This is how blocks in this");
                lore.add(2, "§7theme will look in-game.");
                lore.add(3, "");
                previewMeta.setLore(lore);
                preview.setItemMeta(previewMeta);
            }
            inventory.setItem(50, preview);
        }
    }

    private String formatSchemeName(String scheme) {
        String[] parts = scheme.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private String formatMaterialName(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}