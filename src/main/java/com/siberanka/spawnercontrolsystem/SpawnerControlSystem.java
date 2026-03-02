package com.siberanka.spawnercontrolsystem;

import com.siberanka.spawnercontrolsystem.listeners.SpawnerListener;
import com.siberanka.spawnercontrolsystem.managers.CacheManager;
import com.siberanka.spawnercontrolsystem.managers.ConfigManager;
import com.siberanka.spawnercontrolsystem.managers.DatabaseManager;
import com.siberanka.spawnercontrolsystem.managers.LangManager;
import com.siberanka.spawnercontrolsystem.managers.LoggerManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SpawnerControlSystem extends JavaPlugin {

    private static SpawnerControlSystem instance;
    private ConfigManager configManager;
    private LangManager langManager;
    private CacheManager cacheManager;
    private DatabaseManager databaseManager;
    private LoggerManager loggerManager;

    @Override
    public void onEnable() {
        instance = this;

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe("PlaceholderAPI not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configManager = new ConfigManager(this);
        this.langManager = new LangManager(this);
        this.cacheManager = new CacheManager();
        this.databaseManager = new DatabaseManager(this, cacheManager);
        this.loggerManager = new LoggerManager(this);

        loadPlugin();

        databaseManager.connect();

        getServer().getPluginManager().registerEvents(new SpawnerListener(this), this);

        if (getCommand("scc") != null) {
            getCommand("scc").setExecutor(new com.siberanka.spawnercontrolsystem.commands.SpawnerCommand(this));
        }

        getLogger().info("SpawnerControlSystem enabled! (Folia 1.21.x Ready)");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("SpawnerControlSystem disabled!");
    }

    public void loadPlugin() {
        configManager.load();
        langManager.load(configManager.getLang());
    }

    public static SpawnerControlSystem getInstance() {
        return instance;
    }

    public ConfigManager getConfigurationManager() {
        return configManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public LoggerManager getLoggerManager() {
        return loggerManager;
    }
}
