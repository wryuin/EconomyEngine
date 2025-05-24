package me.wryuin.database;

import me.wryuin.Currency;
import me.wryuin.EconomyEngine;
import me.wryuin.data.PlayerData;
import org.bukkit.OfflinePlayer;
import java.util.Map;
import java.util.UUID;

public interface DataBase extends AutoCloseable {
    void initialize();
    void saveAll();
    void reload();

    boolean createCurrency(String name, String symbol);
    boolean deleteCurrency(String name);
    boolean currencyExists(String name);
    Map<String, Currency> getCurrencies();

    PlayerData loadPlayerData(UUID uuid);
    double getBalance(OfflinePlayer player, String currency);
    void setBalance(OfflinePlayer player, String currency, double amount);
    void addBalance(OfflinePlayer player, String currency, double amount);
    void removeBalance(OfflinePlayer player, String currency, double amount);
    boolean transferBalance(OfflinePlayer from, OfflinePlayer to, String currency, double amount);

    void logTransaction(UUID player, String currency, double amount, String operation);

    @Override
    void close() throws Exception;
}