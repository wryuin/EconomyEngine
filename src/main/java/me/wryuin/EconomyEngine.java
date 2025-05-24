package me.wryuin;

import me.wryuin.commands.EconomyCommand;
import me.wryuin.commands.EconomyTabCompleter;
import me.wryuin.database.*;
import me.wryuin.events.JoinListener;
import me.wryuin.placeholders.EconomyPlaceholders;
import me.wryuin.utils.BackupManager;
import me.wryuin.utils.ConfigManager;
import me.wryuin.utils.Messages;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyEngine extends JavaPlugin {
    private static EconomyEngine instance;
    private DataBase database;
    private ConfigManager configManager;
    private CacheManager cacheManager;
    private BackupManager backupManager;
    private UpdateChecker updateChecker;


    @Override
    public void reloadConfig() {
        super.reloadConfig();
        configManager.onConfigReload();
    }

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        configManager.initialLoad();
        instance = this;
        saveDefaultConfig();
        setupDatabase();
        getCommand("economy").setExecutor(new EconomyCommand(this));
        getCommand("economy").setTabCompleter(new EconomyTabCompleter(this));
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        this.cacheManager = new CacheManager(this);
        this.backupManager = new BackupManager(this);
        this.updateChecker = new UpdateChecker(this);
        saveResource("messages.yml", false);
        Messages.init(this);
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
    public CacheManager getCacheManager() {
        return cacheManager;
    }
}
