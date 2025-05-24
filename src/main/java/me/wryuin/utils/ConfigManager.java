package me.wryuin.utils;

import me.wryuin.EconomyEngine;
import me.wryuin.database.DatabaseType;

public class ConfigManager {
    private final EconomyEngine plugin;

    public int getAutoSaveInterval() {
        return plugin.getConfig().getInt("autosave.interval", 5);
    }

    public ConfigManager(EconomyEngine plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    public DatabaseType getDatabaseType() {
        String type = plugin.getConfig().getString("database.type", "YAML");
        return DatabaseType.valueOf(type.toUpperCase());
    }
}