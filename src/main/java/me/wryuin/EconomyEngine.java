package me.wryuin;

import me.wryuin.commands.EconomyCommand;
import me.wryuin.commands.EconomyTabCompleter;
import me.wryuin.commands.GUICommand;
import me.wryuin.database.*;
import me.wryuin.events.JoinListener;
import me.wryuin.gui.EconomyGUI;
import me.wryuin.gui.GUIListener;
import me.wryuin.placeholders.EconomyPlaceholders;
import me.wryuin.utils.BackupManager;
import me.wryuin.utils.ConfigManager;
import me.wryuin.utils.Messages;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyEngine extends JavaPlugin {
    private static volatile EconomyEngine instance;
    private DataBase database;
    private ConfigManager configManager;
    private CacheManager cacheManager;
    private BackupManager backupManager;
    private UpdateChecker updateChecker;
    private EconomyGUI economyGUI;


    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (configManager != null) {
            configManager.onConfigReload();
        }
        if (economyGUI != null) {
            economyGUI.reloadConfig();
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        configManager.initialLoad();
        saveDefaultConfig();
        
        // Save default resource files
        saveResource("messages.yml", false);
        saveResource("gui.yml", false);
        
        Messages.init(this);
        
        setupDatabase();
        
        if (getCommand("economy") != null) {
            getCommand("economy").setExecutor(new EconomyCommand(this));
            getCommand("economy").setTabCompleter(new EconomyTabCompleter(this));
        }
        
        this.economyGUI = new EconomyGUI(this);
        
        if (getCommand("economygui") != null) {
            getCommand("economygui").setExecutor(new GUICommand(this, economyGUI));
        }
        
        getServer().getPluginManager().registerEvents(new GUIListener(this, economyGUI), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        this.cacheManager = new CacheManager(this);
        this.backupManager = new BackupManager(this);
        this.updateChecker = new UpdateChecker(this);
        
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EconomyPlaceholders(this).register();
        }

        setupAutoSave();

        getLogger().info("EconomyEngine enabled");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.saveAll();
        }
        getLogger().info("EconomyEngine disabled");
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
        if (interval <= 0) {
            interval = 6000;
        }
        
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
    
    public EconomyGUI getEconomyGUI() {
        return economyGUI;
    }
}
