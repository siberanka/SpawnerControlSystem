package com.siberanka.spawnercontrolsystem.managers;

import com.siberanka.spawnercontrolsystem.SpawnerControlSystem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class LangManager {

    private final SpawnerControlSystem plugin;
    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, String> rawMessages = new HashMap<>();

    public LangManager(SpawnerControlSystem plugin) {
        this.plugin = plugin;
    }

    public void load(String langCode) {
        messages.clear();
        rawMessages.clear();

        saveResourceIfNotExists("lang/en.yml");
        saveResourceIfNotExists("lang/tr.yml");

        File langFile = new File(plugin.getDataFolder(), "lang/" + langCode + ".yml");
        if (!langFile.exists()) {
            langFile = new File(plugin.getDataFolder(), "lang/en.yml");
            plugin.getLogger().warning("Language file " + langCode + ".yml not found. Falling back to en.yml");
        }

        YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);

        InputStream defLangStream = plugin.getResource("lang/" + langFile.getName());
        boolean changed = false;
        if (defLangStream != null) {
            YamlConfiguration defConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(defLangStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defConfig);
            langConfig.options().copyDefaults(true);
            try {
                langConfig.options().parseComments(true);
            } catch (NoSuchMethodError ignored) {
            }

            for (String key : langConfig.getKeys(true)) {
                if (!defConfig.contains(key)) {
                    langConfig.set(key, null);
                    changed = true;
                }
            }
        }

        if (changed) {
            try {
                langConfig.save(langFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save " + langFile.getName(), e);
            }
        }

        String prefix = langConfig.getString("prefix", "&8[&eSCC&8] &7» ");

        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isConfigurationSection(key))
                continue;
            if (key.equals("prefix"))
                continue;
            String msg = langConfig.getString(key);
            if (msg != null) {
                messages.put(key, ChatColor.translateAlternateColorCodes('&', prefix + msg));
                rawMessages.put(key, msg);
            }
        }
        messages.put("prefix", ChatColor.translateAlternateColorCodes('&', prefix));
        messages.put("raw_prefix", prefix); // For cases where we just need the prefix without coloring it beforehand or
                                            // for testing
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                plugin.saveResource(resourcePath, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Resource " + resourcePath + " not found to save.");
            }
        }
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, ChatColor.translateAlternateColorCodes('&', "&cMessage not found: " + key));
    }

    public String getRawMessage(String key) {
        return rawMessages.getOrDefault(key, "Message not found: " + key);
    }
}
