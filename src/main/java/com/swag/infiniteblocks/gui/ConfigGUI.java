package com.swag.infiniteblocks.gui;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ConfigGUI {

    private final InfiniteBlocksPlugin plugin;
    private final Player player;
    private final int page;
    private Inventory inventory;

    private static final Map<UUID, ConfigGUI> activeGUIs = new HashMap<>();
    private static final Map<UUID, String> deleteConfirmations = new HashMap<>();

    private static NamespacedKey schemeKey;
    private static NamespacedKey actionKey;

    private static final int SCHEMES_PER_PAGE = 9;
    private static final int GUI_SIZE = 27;

    public ConfigGUI(InfiniteBlocksPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public ConfigGUI(InfiniteBlocksPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        if (schemeKey == null) schemeKey = new NamespacedKey(plugin, "gui_scheme_key");
        if (actionKey == null) actionKey = new NamespacedKey(plugin, "gui_action");
    }

    public void open() {
        createInventory();
        player.openInventory(inventory);
        activeGUIs.put(player.getUniqueId(), this);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public static ConfigGUI getActiveGUI(Player player) {
        return activeGUIs.get(player.getUniqueId());
    }

    public static void removeActiveGUI(Player player) {
        activeGUIs.remove(player.getUniqueId());
        deleteConfirmations.remove(player.getUniqueId());
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        // Handle action buttons (prev/next/add/close)
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            switch (action) {
                case "prev":
                    new ConfigGUI(plugin, player, page - 1).open();
                    return;
                case "next":
                    new ConfigGUI(plugin, player, page + 1).open();
                    return;
                case "add":
                    player.closeInventory();
                    player.sendMessage(org.bukkit.ChatColor.YELLOW + "Use " + org.bukkit.ChatColor.AQUA + "/ib scheme create <name>"
                            + org.bukkit.ChatColor.YELLOW + " to add a new scheme.");
                    return;
                case "close":
                    player.closeInventory();
                    return;
            }
        }

        // Handle scheme buttons
        String scheme = meta.getPersistentDataContainer().get(schemeKey, PersistentDataType.STRING);
        if (scheme == null) return;

        UUID playerId = player.getUniqueId();

        // Right-click = delete
        if (event.getClick() == ClickType.RIGHT) {
            if (scheme.equalsIgnoreCase(plugin.getConfigManager().getDefaultScheme())) {
                player.sendMessage(org.bukkit.ChatColor.RED + "You cannot delete the default scheme!");
                return;
            }

            String confirming = deleteConfirmations.get(playerId);
            if (confirming == null || !confirming.equals(scheme)) {
                deleteConfirmations.put(playerId, scheme);
                player.sendMessage(org.bukkit.ChatColor.RED + "!!! CONFIRM DELETION !!!");
                player.sendMessage(org.bukkit.ChatColor.YELLOW + "Right-click again on '" + scheme + "' to permanently delete it.");
                return;
            }

            try {
                plugin.getConfigManager().removeScheme(scheme);
                deleteConfirmations.remove(playerId);
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Successfully deleted scheme: " + org.bukkit.ChatColor.YELLOW + scheme);
                new ConfigGUI(plugin, player, page).open(); // refresh same page
            } catch (Exception e) {
                player.sendMessage(org.bukkit.ChatColor.RED + "Error deleting scheme: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        // Left-click = manage blocks
        if (event.getClick() == ClickType.LEFT) {
            deleteConfirmations.remove(playerId);
            player.closeInventory();
            new SchemeBlocksGUI(plugin, player, scheme).open();
        }
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(null, GUI_SIZE, org.bukkit.ChatColor.DARK_PURPLE + "InfiniteBlocks Config");

        // Fill with border
        ItemStack border = createFiller(Material.MAGENTA_STAINED_GLASS_PANE);
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, border);
        }

        // Get all schemes and paginate
        List<String> allSchemes = new ArrayList<>(plugin.getConfigManager().getSchemes());
        int totalSchemes = allSchemes.size();
        int fromIndex = page * SCHEMES_PER_PAGE;
        int toIndex = Math.min(fromIndex + SCHEMES_PER_PAGE, totalSchemes);

        if (fromIndex >= totalSchemes && totalSchemes > 0) {
            // Invalid page, go back to last valid page
            new ConfigGUI(plugin, player, Math.max(0, (totalSchemes - 1) / SCHEMES_PER_PAGE)).open();
            return;
        }

        List<String> pageSchemes = allSchemes.subList(fromIndex, toIndex);

        // Center schemes in middle row (slots 9-17)
        int rowStart = 9;
        int maxPerRow = 9;
        int itemsInRow = pageSchemes.size();
        int startSlot = rowStart + ((maxPerRow - itemsInRow) / 2);

        for (int i = 0; i < itemsInRow; i++) {
            String scheme = pageSchemes.get(i);
            inventory.setItem(startSlot + i, createSchemeButton(scheme));
        }

        // Bottom row buttons
        // Slot 18 = Previous (if page > 0)
        if (page > 0) {
            inventory.setItem(18, createActionButton(Material.ARROW, "§e◀ Previous Page", "prev"));
        }

        // Slot 22 = Add Scheme
        inventory.setItem(22, createActionButton(Material.EMERALD, "§a§l+ ADD SCHEME",
                Arrays.asList("§7Click to see instructions", "§7for creating a scheme."), "add"));

        // Slot 26 = Next (if more pages exist)
        if (toIndex < totalSchemes) {
            inventory.setItem(26, createActionButton(Material.ARROW, "§eNext Page ▶", "next"));
        }

        // Slot 24 = Close
        inventory.setItem(24, createActionButton(Material.BARRIER, "§c§lCLOSE",
                Collections.singletonList("§7Return to game"), "close"));
    }

    private ItemStack createSchemeButton(String scheme) {
        Material mat = Material.PAPER;
        String cleanName = formatSchemeName(scheme);
        List<String> hexList = plugin.getConfigManager().getNameGradient(scheme);
        String renderedName = applyGradientIfPossible(cleanName, hexList);

        List<String> lore = new ArrayList<>();
        lore.add("§7Left-Click: §bManage Blocks");
        lore.add("§7Right-Click: §cDelete Scheme");
        lore.add("");
        lore.add("§7Theme ID: §f" + scheme);

        return createButton(mat, renderedName, lore, scheme, null);
    }

    private ItemStack createActionButton(Material mat, String name, String action) {
        return createActionButton(mat, name, Collections.emptyList(), action);
    }

    private ItemStack createActionButton(Material mat, String name, List<String> lore, String action) {
        return createButton(mat, name, lore, null, action);
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

    private String applyGradientIfPossible(String text, List<String> hexList) {
        if (text == null) return "§d§lSCHEME";
        if (text.contains("&x&") || text.contains("&#")) {
            return translateHex(text);
        }
        if (hexList == null || hexList.isEmpty()) {
            return "§d§l" + text;
        }

        List<String> colors = new ArrayList<>();
        for (String h : hexList) {
            if (h == null) continue;
            String s = h.trim();
            if (s.isEmpty()) continue;
            if (!s.startsWith("#") && s.matches("(?i)[0-9a-f]{6}")) s = "#" + s;
            colors.add(s);
        }

        if (colors.isEmpty()) return "§d§l" + text;
        if (colors.size() == 1) {
            try {
                return ChatColor.of(colors.get(0)) + "§l" + text;
            } catch (IllegalArgumentException ignored) {
                return "§d§l" + text;
            }
        }

        return gradient(text, colors);
    }

    private String gradient(String text, List<String> colors) {
        if (text == null || text.isEmpty()) return "";
        int n = text.length();
        StringBuilder out = new StringBuilder(n * 8);

        for (int i = 0; i < n; i++) {
            double t = (n == 1) ? 0.0 : (double) i / (double) (n - 1);
            int segments = colors.size() - 1;
            double scaled = t * segments;
            int seg = Math.min((int) Math.floor(scaled), segments - 1);
            double localT = scaled - seg;

            int[] c1 = hexToRgb(colors.get(seg));
            int[] c2 = hexToRgb(colors.get(seg + 1));

            int r = (int) Math.round(c1[0] + (c2[0] - c1[0]) * localT);
            int g = (int) Math.round(c1[1] + (c2[1] - c1[1]) * localT);
            int b = (int) Math.round(c1[2] + (c2[2] - c1[2]) * localT);

            String hex = String.format("#%02X%02X%02X", r, g, b);
            out.append(ChatColor.of(hex)).append(text.charAt(i));
        }
        return out.toString();
    }

    private int[] hexToRgb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() != 6) return new int[]{255, 255, 255};
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        return new int[]{r, g, b};
    }

    private ItemStack createButton(Material mat, String name, List<String> lore, String schemeKeyValue, String actionKeyValue) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(translateHex(name));
            List<String> tLore = new ArrayList<>();
            for (String s : lore) tLore.add(translateHex(s));
            meta.setLore(tLore);

            if (schemeKeyValue != null) {
                meta.getPersistentDataContainer().set(schemeKey, PersistentDataType.STRING, schemeKeyValue);
            }
            if (actionKeyValue != null) {
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, actionKeyValue);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFiller(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private String translateHex(String text) {
        if (text == null) return "";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + matcher.group(1)).toString();
            matcher.appendReplacement(buffer, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}