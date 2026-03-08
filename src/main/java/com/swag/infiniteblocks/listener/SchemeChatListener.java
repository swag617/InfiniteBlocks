package com.swag.infiniteblocks.listener;

import com.swag.infiniteblocks.InfiniteBlocksPlugin;
import com.swag.infiniteblocks.gui.InfiniteBlocksGUI;
import com.swag.infiniteblocks.manager.SchemeCreationManager;
import com.swag.infiniteblocks.utils.BirdflopParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.IOException;
import java.util.List;

public class SchemeChatListener implements Listener {

    private final InfiniteBlocksPlugin plugin;

    public SchemeChatListener(InfiniteBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        // 1) Handle chat-based search mode first
        if (InfiniteBlocksGUI.isSearching(player)) {
            event.setCancelled(true);
            InfiniteBlocksGUI.stopSearching(player);

            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Search cancelled.");
                Bukkit.getScheduler().runTask(plugin, () -> new InfiniteBlocksGUI(plugin, player, null).open());
                return;
            }

            player.sendMessage(ChatColor.GREEN + "Searching for: " + message);
            Bukkit.getScheduler().runTask(plugin, () -> new InfiniteBlocksGUI(plugin, player, message).open());
            return;
        }

        // 2) Scheme creation flow
        SchemeCreationManager scm = plugin.getSchemeCreationManager();
        if (!scm.isActive(player.getUniqueId())) return;

        event.setCancelled(true);
        SchemeCreationManager.Session session = scm.get(player.getUniqueId());
        String msg = message;

        if (msg.equalsIgnoreCase("cancel")) {
            scm.end(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "Scheme creation cancelled.");
            return;
        }

        switch (session.step) {
            case NAME -> {
                session.displayName = msg;
                session.step = SchemeCreationManager.Step.BIRDFLOP;
                player.sendMessage(ChatColor.YELLOW + "Now paste the Birdflop color string (e.g. &x&7&D&1&4&B&E... ) or a #RRGGBB list.");
                player.sendMessage(ChatColor.GRAY + "Type 'skip' to use default colors.");
            }
            case BIRDFLOP -> {
                // IMPORTANT FIX: allow skip here
                if (msg.equalsIgnoreCase("skip")) {
                    session.birdflopRaw = ""; // forces fallback in saveSession()
                    session.step = SchemeCreationManager.Step.LORE;
                    player.sendMessage(ChatColor.YELLOW + "Using default colors.");
                    player.sendMessage(ChatColor.YELLOW + "Now enter lore lines one per message. Type 'done' when finished or 'skip' to use defaults.");
                    return;
                }

                session.birdflopRaw = msg;
                List<String> hexes = BirdflopParser.parseHexList(msg);
                if (hexes.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Couldn't parse any colors. Paste a valid Birdflop or type 'skip' to use defaults.");
                    return;
                }
                session.step = SchemeCreationManager.Step.LORE;
                player.sendMessage(ChatColor.GREEN + "Parsed " + hexes.size() + " color(s). Primary color: " + hexes.get(0));
                player.sendMessage(ChatColor.YELLOW + "Now enter lore lines one per message. Type 'done' when finished or 'skip' to use defaults.");
            }
            case LORE -> {
                if (msg.equalsIgnoreCase("done") || msg.equalsIgnoreCase("skip")) {
                    saveSession(session, player);
                    scm.end(player.getUniqueId());
                } else {
                    session.lore.add(msg);
                    player.sendMessage(ChatColor.GRAY + "Lore line added. Add another or type 'done' when finished.");
                }
            }
        }
    }

    private void saveSession(SchemeCreationManager.Session session, Player player) {
        List<String> hexes = BirdflopParser.parseHexList(session.birdflopRaw == null ? "" : session.birdflopRaw);
        if (hexes.isEmpty()) {
            hexes = plugin.getConfigManager().getNameGradient(plugin.getConfigManager().getDefaultScheme());
            if (hexes == null || hexes.isEmpty()) hexes = List.of("#FFFFFF");
        }

        String displayName = session.displayName == null || session.displayName.trim().isEmpty()
                ? "INFINITE %block%"
                : session.displayName.trim();

        List<String> lore = session.lore.isEmpty()
                ? List.of("%gradient_line%", "This block can be placed an %gradient_infinite% amount of times.", "%gradient_line%")
                : session.lore;

        try {
            plugin.getConfigManager().addScheme(session.key, displayName, hexes, lore);
            plugin.getConfigManager().reload();
            player.sendMessage(ChatColor.GREEN + "Scheme '" + session.key + "' saved. Primary color: " + hexes.get(0));
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "Failed to save scheme: " + e.getMessage());
            plugin.getLogger().severe("Failed to save scheme: " + session.key);
            e.printStackTrace();
        }
    }
}