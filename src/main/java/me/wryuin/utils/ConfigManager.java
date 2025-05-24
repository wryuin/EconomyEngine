package me.wryuin.utils;

import me.wryuin.EconomyEngine;
import me.wryuin.database.DatabaseType;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final EconomyEngine plugin;
    private final FileConfiguration config;

    public ConfigManager(EconomyEngine plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public DatabaseType getDatabaseType() {
        String type = config.getString("database.type", "YAML").toUpperCase();
        try {
            return DatabaseType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return DatabaseType.YAML;
        }
    }

    public int getAutoSaveInterval() {
        return config.getInt("autosave.interval", 5);
    }

    public void setupDefaultConfig() {
        config.addDefault("database.type", "YAML");
        config.addDefault("autosave.interval", 5);
        config.addDefault("mysql.host", "localhost");
        config.addDefault("mysql.port", 3306);
        config.addDefault("mysql.database", "economy");
        config.addDefault("mysql.username", "user");
        config.addDefault("mysql.password", "password");

        plugin.saveConfig();
    }
}
