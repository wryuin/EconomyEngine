package me.wryuin.database;

import me.wryuin.Currency;
import me.wryuin.EconomyEngine;
import org.bukkit.OfflinePlayer;
import me.wryuin.data.PlayerData;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.LinkedHashMap;

public class SQLiteDatabase implements DataBase {
    private final EconomyEngine plugin;
    private Connection connection;
    private FileConfiguration dataConfig;

    public SQLiteDatabase(EconomyEngine plugin) {
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
    public void reload() {
        initialize();
    }

    @Override
    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/economy.db");

            createTables();
            plugin.getLogger().info("SQLite database connected successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to SQLite database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Table for currencies
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS currencies (" +
                    "name TEXT PRIMARY KEY," +
                    "symbol TEXT NOT NULL)");

            // Table for balances
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS balances (" +
                    "player_uuid TEXT NOT NULL," +
                    "currency_name TEXT NOT NULL," +
                    "amount REAL NOT NULL," +
                    "PRIMARY KEY (player_uuid, currency_name)," +
                    "FOREIGN KEY (currency_name) REFERENCES currencies(name))");

            // Table for logs
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transaction_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid TEXT NOT NULL," +
                    "currency_name TEXT NOT NULL," +
                    "amount REAL NOT NULL," +
                    "operation TEXT NOT NULL," +
                    "timestamp BIGINT NOT NULL)");
        }
    }

    @Override
    public void saveAll() {
        // Automatic
    }

    @Override
    public boolean createCurrency(String name, String symbol) {
        if (currencyExists(name)) return false;

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO currencies (name, symbol) VALUES (?, ?)")) {
            stmt.setString(1, name);
            stmt.setString(2, symbol);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create currency " + name, e);
            return false;
        }
    }

    @Override
    public boolean deleteCurrency(String name) {
        if (!currencyExists(name)) return false;

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM balances WHERE currency_name = ?")) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM transaction_logs WHERE currency_name = ?")) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM currencies WHERE name = ?")) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to rollback transaction", ex);
            }
            plugin.getLogger().log(Level.WARNING, "Failed to delete currency " + name, e);
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to reset auto-commit", e);
            }
        }
    }

    @Override
    public boolean currencyExists(String name) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM currencies WHERE name = ?")) {
            stmt.setString(1, name);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check currency existence " + name, e);
            return false;
        }
    }

    @Override
    public Map<String, Currency> getCurrencies() {
        Map<String, Currency> currencies = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, symbol FROM currencies")) {
            while (rs.next()) {
                String name = rs.getString("name");
                currencies.put(name,
                        new Currency(name, name, rs.getString("symbol")));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get currencies", e);
        }
        return currencies;
    }

    @Override
    public double getBalance(OfflinePlayer player, String currency) {
        if (!currencyExists(currency)) return 0;

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT amount FROM balances WHERE player_uuid = ? AND currency_name = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, currency);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble("amount") : 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to get balance for " + player.getName() + " currency " + currency, e);
            return 0;
        }
    }

    @Override
    public void setBalance(OfflinePlayer player, String currency, double amount) {
        if (!currencyExists(currency)) return;

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO balances (player_uuid, currency_name, amount) VALUES (?, ?, ?)")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, currency);
            stmt.setDouble(3, amount);
            stmt.executeUpdate();
            logTransaction(player.getUniqueId(), currency, amount, "SET");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to set balance for " + player.getName() + " currency " + currency, e);
        }
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

        try {
            connection.setAutoCommit(false);

            double fromBalance = getBalance(from, currency);
            if (fromBalance < amount) {
                connection.rollback();
                return false;
            }

            removeBalance(from, currency, amount);
            addBalance(to, currency, amount);

            connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to rollback transaction", ex);
            }
            plugin.getLogger().log(Level.WARNING,
                    "Failed to transfer " + amount + " " + currency + " from " +
                            from.getName() + " to " + to.getName(), e);
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to reset auto-commit", e);
            }
        }
    }

    @Override
    public void logTransaction(UUID player, String currency, double amount, String operation) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO transaction_logs (player_uuid, currency_name, amount, operation, timestamp) " +
                        "VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, player.toString());
            stmt.setString(2, currency);
            stmt.setDouble(3, amount);
            stmt.setString(4, operation);
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to log transaction for " + player + " currency " + currency, e);
        }
    }

    @Override
    public Map<UUID, Double> getTopBalances(String currency, int limit) {
        Map<UUID, Double> topBalances = new LinkedHashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT player_uuid, amount FROM balances WHERE currency_name = ? ORDER BY amount DESC LIMIT ?")) {
            stmt.setString(1, currency);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                topBalances.put(UUID.fromString(rs.getString("player_uuid")), rs.getDouble("amount"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get top balances for currency " + currency, e);
        }
        return topBalances;
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}