package com.swag.infiniteblocks;

import com.swag.infiniteblocks.command.InfBlocksCommand;
import com.swag.infiniteblocks.config.ConfigManager;
import com.swag.infiniteblocks.listener.AddBlockListener;
import com.swag.infiniteblocks.listener.BlockPlaceListener;
import com.swag.infiniteblocks.listener.GUIListener;
import com.swag.infiniteblocks.listener.HandSwapListener;
import com.swag.infiniteblocks.listener.SearchListener;
import com.swag.infiniteblocks.listener.SchemeChatListener;
import com.swag.infiniteblocks.manager.InfiniteBlockManager;
import com.swag.infiniteblocks.manager.SchemeCreationManager;
import org.bukkit.plugin.java.JavaPlugin;

public class InfiniteBlocksPlugin extends JavaPlugin {

    private static InfiniteBlocksPlugin instance;
    private ConfigManager configManager;
    private InfiniteBlockManager infiniteBlockManager;
    private SchemeCreationManager schemeCreationManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        configManager = new ConfigManager(this);
        infiniteBlockManager = new InfiniteBlockManager(this);
        schemeCreationManager = new SchemeCreationManager();

        InfBlocksCommand infCmd = new InfBlocksCommand(this);
        if (getCommand("infblocks") != null) {
            getCommand("infblocks").setExecutor(infCmd);
            getCommand("infblocks").setTabCompleter(infCmd);
        } else {
            getLogger().warning("Command 'infblocks' not defined in plugin.yml");
        }

        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new HandSwapListener(this), this);
        getServer().getPluginManager().registerEvents(new SearchListener(this), this);
        getServer().getPluginManager().registerEvents(new AddBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new SchemeChatListener(this), this);

        getLogger().info("InfiniteBlocks has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("InfiniteBlocks has been disabled!");
    }

    public static InfiniteBlocksPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public InfiniteBlockManager getInfiniteBlockManager() {
        return infiniteBlockManager;
    }

    public SchemeCreationManager getSchemeCreationManager() {
        return schemeCreationManager;
    }
}