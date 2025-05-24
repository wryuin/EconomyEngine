package me.wryuin;

import me.wryuin.commands.EconomyCommand;
import me.wryuin.database.*;
import me.wryuin.events.JoinListener;
import me.wryuin.placeholders.EconomyPlaceholders;
import me.wryuin.utils.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyEngine extends JavaPlugin {
    private static EconomyEngine instance;
    private DataBase database;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        setupDatabase();
        getCommand("economy").setExecutor(new EconomyCommand(this));
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EconomyPlaceholders(this).register();
        }

        setupAutoSave();

        getLogger().info("EconomyEngine успешно запущен!");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.saveAll();
        }
        getLogger().info("EconomyEngine успешно выключен!");
    }

    private void setupDatabase() {
        DatabaseType type = configManager.getDatabaseType();
        switch (type) {
            case YAML:
                database = new YamlDatabase(this);
                break;
            case SQLITE:
                database = new SQLiteDatabase(this);
                break;
            case MYSQL:
                database = new MySQLDatabase(this);
                break;
            default:
                database = new YamlDatabase(this);
        }
        database.initialize();
    }

    private void setupAutoSave() {
        int interval = configManager.getAutoSaveInterval() * 60 * 20;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (database != null) {
                database.saveAll();
                getLogger().info("Данные успешно сохранены (автосохранение)");
            }
        }, interval, interval);
    }

    public static EconomyEngine getInstance() {
        return instance;
    }

    public DataBase getDatabase() {
        return database;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
