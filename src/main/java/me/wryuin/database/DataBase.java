package me.wryuin.database;

import me.wryuin.Currency;
import me.wryuin.EconomyEngine;
import me.wryuin.data.PlayerData;
import org.bukkit.OfflinePlayer;
import java.util.Map;
import java.util.UUID;
import java.util.List;


public interface DataBase extends AutoCloseable {
    void initialize();
    void saveAll();
    void reload();

    boolean createCurrency(String name, String symbol);
    boolean deleteCurrency(String name);
    boolean currencyExists(String name);
    Map<String, Currency> getCurrencies();

    default void batchLogTransactions(List<Transaction> transactions) {
        transactions.forEach(t -> logTransaction(t.player(), t.currency(), t.amount(), t.operation()));
    }

    PlayerData loadPlayerData(UUID uuid);
    double getBalance(OfflinePlayer player, String currency);
    void setBalance(OfflinePlayer player, String currency, double amount);
    void addBalance(OfflinePlayer player, String currency, double amount);
    void removeBalance(OfflinePlayer player, String currency, double amount);
    boolean transferBalance(OfflinePlayer from, OfflinePlayer to, String currency, double amount);

    void logTransaction(UUID player, String currency, double amount, String operation);

    Map<UUID, Double> getTopBalances(String currency, int limit);

    @Override
    void close() throws Exception;
}