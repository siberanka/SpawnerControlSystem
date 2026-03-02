package com.siberanka.spawnercontrolsystem.managers;

import com.siberanka.spawnercontrolsystem.SpawnerControlSystem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {

    private final SpawnerControlSystem plugin;
    private FileConfiguration config;

    private String lang;

    private boolean dbEnabled;
    private String dbFileName;

    private boolean placeChecksEnabled;
    private List<String> placeWorlds;
    private List<List<String>> placeConditionGroups;

    private boolean breakChecksEnabled;
    private List<String> breakWorlds;
    private List<List<String>> breakConditionGroups;

    public ConfigManager(SpawnerControlSystem plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig();
        config = plugin.getConfig();

        updateConfig(configFile);

        lang = config.getString("lang", "en");

        dbEnabled = config.getBoolean("database.enabled", true);
        dbFileName = config.getString("database.file-name", "spawners.db");

        placeChecksEnabled = config.getBoolean("place-conditions.enabled", true);
        placeWorlds = config.getStringList("place-conditions.enabled-worlds");
        if (placeWorlds.isEmpty())
            placeWorlds.add("*"); // Fallback

        placeConditionGroups = new ArrayList<>();
        ConfigurationSection placeGroupsSec = config.getConfigurationSection("place-conditions.condition-groups");
        if (placeGroupsSec != null) {
            for (String key : placeGroupsSec.getKeys(false)) {
                placeConditionGroups.add(placeGroupsSec.getStringList(key));
            }
        }

        breakChecksEnabled = config.getBoolean("break-conditions.enabled", true);
        breakWorlds = config.getStringList("break-conditions.enabled-worlds");
        if (breakWorlds.isEmpty())
            breakWorlds.add("*"); // Fallback

        breakConditionGroups = new ArrayList<>();
        ConfigurationSection breakGroupsSec = config.getConfigurationSection("break-conditions.condition-groups");
        if (breakGroupsSec != null) {
            for (String key : breakGroupsSec.getKeys(false)) {
                breakConditionGroups.add(breakGroupsSec.getStringList(key));
            }
        }
    }

    private void updateConfig(File configFile) {
        InputStream defConfigStream = plugin.getResource("config.yml");
        if (defConfigStream == null)
            return;

        YamlConfiguration defConfig = YamlConfiguration
                .loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));

        config.setDefaults(defConfig);
        config.options().copyDefaults(true);
        try {
            config.options().parseComments(true);
        } catch (NoSuchMethodError ignored) {
            // parseComments might not be available on older versions, but 1.21 should have
            // it
        }

        boolean changed = false;

        for (String key : config.getKeys(true)) {
            if (!defConfig.contains(key)) {
                config.set(key, null);
                changed = true;
            }
        }

        if (changed || config.getKeys(false).isEmpty()) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", e);
            }
        }
    }

    public String getLang() {
        return lang;
    }

    public boolean isDbEnabled() {
        return dbEnabled;
    }

    public String getDbFileName() {
        return dbFileName;
    }

    public boolean isPlaceChecksEnabled() {
        return placeChecksEnabled;
    }

    public List<String> getPlaceWorlds() {
        return placeWorlds;
    }

    public List<List<String>> getPlaceConditionGroups() {
        return placeConditionGroups;
    }

    public boolean isBreakChecksEnabled() {
        return breakChecksEnabled;
    }

    public List<String> getBreakWorlds() {
        return breakWorlds;
    }

    public List<List<String>> getBreakConditionGroups() {
        return breakConditionGroups;
    }
}
