package com.swag.infiniteblocks.manager;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import com.swag.infiniteblocks.utils.GradientUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class InfiniteBlockManager {

    private final InfiniteBlocksPlugin plugin;
    private final NamespacedKey infiniteBlockKey;
    private final NamespacedKey legacyInfiniteKey;

    // Plantable/placeable items that return false for isBlock() but still fire BlockPlaceEvent
    private static final Set<Material> PLACEABLE_NON_BLOCK_ITEMS;
    static {
        Set<Material> set = new HashSet<>();
        for (String name : new String[]{
                "WHEAT_SEEDS", "BEETROOT_SEEDS", "MELON_SEEDS", "PUMPKIN_SEEDS",
                "TORCHFLOWER_SEEDS", "PITCHER_POD",
                "NETHER_WART", "SWEET_BERRIES", "GLOW_BERRIES", "COCOA_BEANS"
        }) {
            Material m = Material.getMaterial(name);
            if (m != null && m.isItem() && !m.isBlock()) {
                set.add(m);
            }
        }
        PLACEABLE_NON_BLOCK_ITEMS = Collections.unmodifiableSet(set);
    }

    public InfiniteBlockManager(InfiniteBlocksPlugin plugin) {
        this.plugin = plugin;
        this.infiniteBlockKey = new NamespacedKey(plugin, "infinite_block");
        this.legacyInfiniteKey = new NamespacedKey("infiniteblocks", "infinite");
    }

    public NamespacedKey getInfiniteBlockKey() {
        return infiniteBlockKey;
    }

    public boolean isInfiniteBlock(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(infiniteBlockKey, PersistentDataType.BYTE) ||
                pdc.has(legacyInfiniteKey, PersistentDataType.SHORT) ||
                pdc.has(legacyInfiniteKey, PersistentDataType.BYTE);
    }

    public ItemStack createInfiniteBlock(Material material, String scheme) {
        if (material == null || !material.isItem() || material.isAir()) return null;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        String blockName = formatMaterialName(material);

        // 1. Get Name Template and Colors
        String nameTemplate = plugin.getConfigManager().getItemName(scheme);
        if (nameTemplate == null || !nameTemplate.contains("%block%")) {
            nameTemplate = "INFINITE %block%";
        }

        List<String> hexList = plugin.getConfigManager().getNameGradient(scheme);
        List<Color> colors = new ArrayList<>();
        if (hexList != null) {
            for (String hex : hexList) {
                try {
                    colors.add(Color.decode(hex.startsWith("#") ? hex : "#" + hex));
                } catch (Exception ignored) {}
            }
        }

        // 2. Apply Name Gradient (Replace placeholder FIRST)
        String fullTextToGradient = stripLegacyFormatting(nameTemplate.replace("%block%", blockName));
        String finalDisplayName;
        if (colors.size() >= 2) {
            String gradientResult = GradientUtil.applyGradient(fullTextToGradient, colors);
            // Force bold after every hex code so it persists
            finalDisplayName = translateAllColors(forceBoldPerHexToken(gradientResult));
        } else {
            String primary = (hexList != null && !hexList.isEmpty()) ? hexList.get(0) : "#FF55FF";
            finalDisplayName = translateAllColors(toHashToken(primary) + "&l" + fullTextToGradient);
        }
        meta.setDisplayName(finalDisplayName);

        // 3. Process Lore
        List<String> rawLore = plugin.getConfigManager().getItemLore(scheme);
        if (rawLore != null) {
            List<String> finalLore = new ArrayList<>();

            String primary = (hexList != null && !hexList.isEmpty()) ? hexList.get(0) : "#FF55FF";
            String primaryColor = translateAllColors(toHashToken(primary)); // Translated once

            String gradLine = (colors.size() >= 2)
                    ? translateAllColors(GradientUtil.applyGradient("----------------------", colors))
                    : primaryColor + "----------------------";

            String infiniteWord = (colors.size() >= 2)
                    ? translateAllColors(forceBoldPerHexToken(GradientUtil.applyGradient("INFINITE", colors)))
                    : primaryColor + "§lINFINITE";

            for (String line : rawLore) {
                // Start every line with primary color
                String processed = primaryColor + line;

                // Replace placeholders
                processed = processed.replace("%block%", blockName)
                        .replace("%gradient_line%", gradLine)
                        // After INFINITE, return to primary color (no reset needed, just re-apply primary)
                        .replace("%gradient_infinite%", infiniteWord + primaryColor);

                finalLore.add(processed);
            }
            meta.setLore(finalLore);
        }
        // 4. Glow + PDC Tag
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(infiniteBlockKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public List<Material> getAllPlaceableBlocks() {
        return Arrays.stream(Material.values())
                .filter(m -> (m.isBlock() && m.isItem() && !m.isAir())
                        || PLACEABLE_NON_BLOCK_ITEMS.contains(m))
                .sorted(Comparator.comparing(Enum::name))
                .collect(Collectors.toList());
    }

    public List<Material> searchBlocks(String query) {
        String lowerQuery = query.toLowerCase();
        return getAllPlaceableBlocks().stream()
                .filter(m -> m.name().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    /** Returns true if this material is a plantable/placeable item (e.g. seeds). */
    public boolean isPlaceableNonBlock(Material material) {
        return PLACEABLE_NON_BLOCK_ITEMS.contains(material);
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

    private String translateAllColors(String text) {
        if (text == null || text.isEmpty()) return "";
        String out = text;
        out = convertHashTokenToSectionX(out);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', out);
    }

    private String convertHashTokenToSectionX(String text) {
        Pattern p = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher m = p.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder sectionHex = new StringBuilder("§x");
            for (char ch : hex.toCharArray()) {
                sectionHex.append('§').append(ch);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(sectionHex.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String toHashToken(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        return "&#" + h;
    }

    private String stripLegacyFormatting(String text) {
        if (text == null) return "";
        return org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', text));
    }

    private String forceBoldPerHexToken(String gradientText) {
        if (gradientText == null || gradientText.isEmpty()) return "";
        return gradientText.replaceAll("&#([A-Fa-f0-9]{6})", "&#$1&l");
    }
}