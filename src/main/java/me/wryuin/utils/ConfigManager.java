package me.wryuin.utils;

import me.wryuin.EconomyEngine;
import me.wryuin.database.DatabaseType;
import org.bukkit.configuration.ConfigurationSection;

public class ConfigManager {
    private final EconomyEngine plugin;
    private DatabaseType databaseType;
    private int autoSaveInterval;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;


    public ConfigManager(EconomyEngine plugin) {
        this.plugin = plugin;
    }

    public void initialLoad() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        loadSettings();
    }

    public void setup() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    public void onConfigReload() {
        loadSettings();
    }

    public void loadSettings() {
        try {
            // 1. Тип базы данных
            String dbTypeRaw = plugin.getConfig().getString("database.type", "YAML").toUpperCase();
            try {
                this.databaseType = DatabaseType.valueOf(dbTypeRaw);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестный тип БД: " + dbTypeRaw + ". Используем YAML");
                this.databaseType = DatabaseType.YAML;
            }

            // 2. Интервал автосохранения (минуты -> тики)
            this.autoSaveInterval = plugin.getConfig().getInt("autosave.interval", 5) * 1200;

            // 3. Настройки MySQL
            ConfigurationSection mysqlSection = plugin.getConfig().getConfigurationSection("mysql");
            if (mysqlSection != null) {
                this.mysqlHost = mysqlSection.getString("host", "localhost");
                this.mysqlPort = mysqlSection.getInt("port", 3306);
                this.mysqlDatabase = mysqlSection.getString("database", "economy");
                this.mysqlUsername = mysqlSection.getString("username", "root");
                this.mysqlPassword = mysqlSection.getString("password", "");
            } else {
                plugin.getLogger().warning("Секция MySQL не найдена в конфиге!");
            }

            // 4. Валидация параметров
            validateSettings();

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка загрузки конфига: " + e.getMessage());
            plugin.getLogger().severe("Используем настройки по умолчанию");
            setDefaultSettings();
        }
    }

    private void validateSettings() {
        // Проверка интервала автосохранения
        if (autoSaveInterval < 600) { // Минимум 30 секунд
            plugin.getLogger().warning("Слишком маленький интервал автосохранения: "
                    + autoSaveInterval + " тиков. Установлено 5 минут");
            autoSaveInterval = 5 * 1200;
        }

        // Проверка MySQL порта
        if (mysqlPort < 1 || mysqlPort > 65535) {
            plugin.getLogger().warning("Некорректный порт MySQL: " + mysqlPort + ". Используем 3306");
            mysqlPort = 3306;
        }
    }

    private void setDefaultSettings() {
        this.databaseType = DatabaseType.YAML;
        this.autoSaveInterval = 5 * 1200;
        this.mysqlHost = "localhost";
        this.mysqlPort = 3306;
        this.mysqlDatabase = "economy";
        this.mysqlUsername = "root";
        this.mysqlPassword = "";
    }

    public DatabaseType getDatabaseType() { return databaseType; }
    public int getAutoSaveInterval() { return autoSaveInterval; }
    public String getMysqlHost() { return mysqlHost; }
    public int getMysqlPort() { return mysqlPort; }
    public String getMysqlDatabase() { return mysqlDatabase; }
    public String getMysqlUsername() { return mysqlUsername; }
    public String getMysqlPassword() { return mysqlPassword; }
}