package com.swag.infiniteblocks.config;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigManager {

    private final InfiniteBlocksPlugin plugin;

    // Unified storage
    private final Map<String, List<String>> schemeBlocks = new HashMap<>();
    private final Map<String, String> schemeItemName = new HashMap<>();
    private final Map<String, List<String>> schemeItemLore = new HashMap<>();
    private final Map<String, List<String>> schemeNameGradient = new HashMap<>();

    private File schemesFile;
    private FileConfiguration schemesConfig;

    public ConfigManager(InfiniteBlocksPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.getLogger().info("=== RELOAD START ===");

        // Clear ALL maps
        schemeBlocks.clear();
        schemeItemName.clear();
        schemeItemLore.clear();
        schemeNameGradient.clear();
        plugin.getLogger().info("✓ Cleared all maps");

        loadSchemesFile();
        plugin.getLogger().info("✓ Loaded schemes.yml");

        // COMPREHENSIVE DEBUG OUTPUT
        plugin.getLogger().info("=== FINAL STATE ===");
        plugin.getLogger().info("Total schemes loaded: " + schemeNameGradient.size());
        for (String scheme : schemeNameGradient.keySet()) {
            List<String> colors = schemeNameGradient.get(scheme);
            List<String> blocks = schemeBlocks.get(scheme);
            plugin.getLogger().info("  - " + scheme + ":");
            plugin.getLogger().info("      Colors: " + colors.size() + " defined");
            plugin.getLogger().info("      Blocks: " + (blocks != null ? blocks.size() : 0) + " assigned");
        }
        plugin.getLogger().info("=== RELOAD END ===");
    }

    private void loadSchemesFile() {
        schemesFile = new File(plugin.getDataFolder(), "schemes.yml");
        plugin.getLogger().info("--- Loading schemes.yml ---");
        plugin.getLogger().info("File path: " + schemesFile.getAbsolutePath());

        if (!schemesFile.exists()) {
            plugin.getLogger().warning("schemes.yml doesn't exist, creating from resource");
            plugin.saveResource("schemes.yml", false);
        }

        // CRITICAL: Force reload from disk
        schemesConfig = YamlConfiguration.loadConfiguration(schemesFile);

        ConfigurationSection root = schemesConfig.getConfigurationSection("schemes");
        if (root == null) {
            plugin.getLogger().warning("No 'schemes' section found in schemes.yml!");
            return;
        }

        Set<String> schemeKeys = root.getKeys(false);
        plugin.getLogger().info("Found " + schemeKeys.size() + " schemes in file");

        for (String scheme : schemeKeys) {
            String itemName = root.getString(scheme + ".item-name", "INFINITE %block%");
            List<String> nameGradient = root.getStringList(scheme + ".name-gradient");
            List<String> lore = root.getStringList(scheme + ".item-lore");
            List<String> blocks = root.getStringList(scheme + ".blocks");

            plugin.getLogger().info("Loading scheme: " + scheme);

            // Normalize hex colors
            List<String> normalized = new ArrayList<>();
            for (String h : nameGradient) {
                if (h == null) continue;
                String s = h.trim().replace("&", "").replace("x", "");
                if (!s.startsWith("#")) s = "#" + s;
                normalized.add(s.toUpperCase());
            }

            schemeItemName.put(scheme, itemName);
            schemeNameGradient.put(scheme, normalized);
            schemeItemLore.put(scheme, (lore == null || lore.isEmpty()) ?
                    Arrays.asList("%gradient_line%", "This block can be placed an %gradient_infinite% amount of times.", "%gradient_line%") : lore);
            schemeBlocks.put(scheme, blocks != null ? new ArrayList<>(blocks) : new ArrayList<>());

            plugin.getLogger().info("  - Loaded " + (blocks != null ? blocks.size() : 0) + " blocks for " + scheme);
        }
    }

    private void saveSchemes() {
        for (String scheme : schemeNameGradient.keySet()) {
            String base = "schemes." + scheme;
            schemesConfig.set(base + ".item-name", schemeItemName.get(scheme));
            schemesConfig.set(base + ".name-gradient", schemeNameGradient.get(scheme));
            schemesConfig.set(base + ".item-lore", schemeItemLore.get(scheme));
            schemesConfig.set(base + ".blocks", schemeBlocks.get(scheme));
        }

        try {
            schemesConfig.save(schemesFile);
            plugin.getLogger().info("Saved schemes.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save schemes.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getDefaultScheme() {
        return schemesConfig != null ? schemesConfig.getString("settings.default-scheme", "server_colors") : "server_colors";
    }

    public String getItemName(String scheme) {
        return schemeItemName.getOrDefault(scheme, "INFINITE %block%");
    }

    public List<String> getItemLore(String scheme) {
        return schemeItemLore.getOrDefault(scheme, Arrays.asList("%gradient_line%", "This block can be placed an %gradient_infinite% amount of times.", "%gradient_line%"));
    }

    public List<String> getNameGradient(String scheme) {
        return schemeNameGradient.getOrDefault(scheme, schemeNameGradient.getOrDefault(getDefaultScheme(), new ArrayList<>()));
    }

    public String getSchemeDisplayName(String scheme) {
        return getItemName(scheme);
    }

    public Set<String> getSchemes() {
        return new HashSet<>(schemeNameGradient.keySet());
    }

    public List<String> getSchemeBlocks(String scheme) {
        return new ArrayList<>(schemeBlocks.getOrDefault(scheme, new ArrayList<>()));
    }

    public void addBlockToScheme(String scheme, String materialName) {
        // Remove from any other scheme first
        for (List<String> blocks : schemeBlocks.values()) {
            blocks.remove(materialName);
        }
        schemeBlocks.computeIfAbsent(scheme, k -> new ArrayList<>()).add(materialName);
        saveSchemes();
        plugin.getLogger().info("Added " + materialName + " to scheme " + scheme);
    }

    public void removeBlockFromScheme(String scheme, String materialName) {
        List<String> blocks = schemeBlocks.get(scheme);
        if (blocks != null) {
            blocks.remove(materialName);
            saveSchemes();
            plugin.getLogger().info("Removed " + materialName + " from scheme " + scheme);
        }
    }

    public void addScheme(String key, String itemName, List<String> nameGradient, List<String> lore) throws IOException {
        schemeItemName.put(key, itemName);
        schemeNameGradient.put(key, nameGradient);
        schemeItemLore.put(key, lore);
        schemeBlocks.putIfAbsent(key, new ArrayList<>());

        String base = "schemes." + key;
        schemesConfig.set(base + ".item-name", itemName);
        schemesConfig.set(base + ".name-gradient", nameGradient);
        schemesConfig.set(base + ".item-lore", lore);
        schemesConfig.set(base + ".blocks", new ArrayList<String>());
        schemesConfig.save(schemesFile);

        plugin.getLogger().info("Added new scheme: " + key);
    }

    public void removeScheme(String key) throws IOException {
        if (key == null || key.equalsIgnoreCase(getDefaultScheme())) {
            plugin.getLogger().warning("Cannot remove default scheme: " + key);
            return;
        }
        schemesConfig.set("schemes." + key, null);
        schemesConfig.save(schemesFile);
        reload();

        plugin.getLogger().info("Removed scheme: " + key);
    }

    public String getSchemeForBlock(String materialName) {
        for (Map.Entry<String, List<String>> entry : schemeBlocks.entrySet()) {
            if (entry.getValue().contains(materialName)) {
                return entry.getKey();
            }
        }
        return getDefaultScheme();
    }
}