package me.wryuin.database;

import me.wryuin.Currency;
import me.wryuin.EconomyEngine;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import me.wryuin.data.PlayerData;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class YamlDatabase implements DataBase {
    private final EconomyEngine plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final Map<String, Currency> currencies = new HashMap<>();
    public YamlDatabase(EconomyEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        ConfigurationSection playerSection = dataConfig.getConfigurationSection("players." + uuid);
        if (playerSection != null) {
            ConfigurationSection balances = playerSection.getConfigurationSection("balances");
            if (balances != null) {
                for (String currency : balances.getKeys(false)) {
                    data.setBalance(currency, balances.getDouble(currency));
                }
            }
        }
        return data;
    }

    @Override
    public void initialize() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            plugin.saveResource("data.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadCurrencies();
    }

    @Override
    public void saveAll() {
        saveCurrencies();
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить data.yml: " + e.getMessage());
        }
    }

    private void loadCurrencies() {
        currencies.clear();
        ConfigurationSection currenciesSection = dataConfig.getConfigurationSection("currencies");
        if (currenciesSection != null) {
            for (String currencyName : currenciesSection.getKeys(false)) {
                String symbol = currenciesSection.getString(currencyName + ".symbol", "$");
                currencies.put(currencyName, new Currency(currencyName, symbol));
            }
        }
    }
    public void reload() {
        loadCurrencies();
        try {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        } catch(Exception e) {
            plugin.getLogger().severe("Failed to reload YAML data");
        }
    }
    private void saveCurrencies() {
        ConfigurationSection currenciesSection = dataConfig.createSection("currencies");
        for (Currency currency : currencies.values()) {
            currenciesSection.set(currency.getName() + ".symbol", currency.getSymbol());
        }
    }

    @Override
    public boolean createCurrency(String name, String symbol) {
        if (currencyExists(name)) return false;
        currencies.put(name, new Currency(name, symbol));
        return true;
    }

    @Override
    public boolean deleteCurrency(String name) {
        if (!currencyExists(name)) return false;
        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String playerId : playersSection.getKeys(false)) {
                playersSection.set(playerId + ".balances." + name, null);
            }
        }

        currencies.remove(name);
        return true;
    }

    @Override
    public boolean currencyExists(String name) {
        return currencies.containsKey(name);
    }

    @Override
    public Map<String, Currency> getCurrencies() {
        return new HashMap<>(currencies);
    }

    @Override
    public double getBalance(OfflinePlayer player, String currency) {
        if (!currencyExists(currency)) return 0;
        return dataConfig.getDouble("players." + player.getUniqueId() + ".balances." + currency, 0);
    }

    @Override
    public void setBalance(OfflinePlayer player, String currency, double amount) {
        if (!currencyExists(currency)) return;
        dataConfig.set("players." + player.getUniqueId() + ".balances." + currency, amount);
        logTransaction(player.getUniqueId(), currency, amount, "SET");
    }

    @Override
    public void addBalance(OfflinePlayer player, String currency, double amount) {
        if (!currencyExists(currency)) return;
        double current = getBalance(player, currency);
        setBalance(player, currency, current + amount);
        logTransaction(player.getUniqueId(), currency, amount, "ADD");
    }

    @Override
    public void removeBalance(OfflinePlayer player, String currency, double amount) {
        if (!currencyExists(currency)) return;
        double current = getBalance(player, currency);
        setBalance(player, currency, Math.max(0, current - amount));
        logTransaction(player.getUniqueId(), currency, amount, "REMOVE");
    }

    @Override
    public boolean transferBalance(OfflinePlayer from, OfflinePlayer to, String currency, double amount) {
        if (!currencyExists(currency)) return false;
        double fromBalance = getBalance(from, currency);
        if (fromBalance < amount) return false;

        removeBalance(from, currency, amount);
        addBalance(to, currency, amount);

        logTransaction(from.getUniqueId(), currency, -amount, "TRANSFER_TO_" + to.getUniqueId());
        logTransaction(to.getUniqueId(), currency, amount, "TRANSFER_FROM_" + from.getUniqueId());
        return true;
    }

    @Override
    public void logTransaction(UUID player, String currency, double amount, String operation) {
        long timestamp = System.currentTimeMillis();
        String logKey = "logs." + player + "." + timestamp;

        dataConfig.set(logKey + ".currency", currency);
        dataConfig.set(logKey + ".amount", amount);
        dataConfig.set(logKey + ".operation", operation);
    }
    @Override
    public void close() throws Exception {
        saveAll();
    }
}