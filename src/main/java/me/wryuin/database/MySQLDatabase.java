package me.wryuin.database;


import me.wryuin.Currency;
import me.wryuin.EconomyEngine;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class MySQLDatabase implements DataBase {
    private final EconomyEngine plugin;
    private Connection connection;

    public MySQLDatabase(EconomyEngine plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    @Override
    public void initialize() {
        try {
            String host = getConfig().getString("mysql.host");
            int port = getConfig().getInt("mysql.port");
            String database = getConfig().getString("mysql.database");
            String username = getConfig().getString("mysql.username");
            String password = getConfig().getString("mysql.password");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";
            connection = DriverManager.getConnection(url, username, password);

            createTables();
            plugin.getLogger().info("MySQL database connected successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS currencies (" +
                    "name VARCHAR(32) PRIMARY KEY," +
                    "symbol VARCHAR(5) NOT NULL)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS balances (" +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "currency_name VARCHAR(32) NOT NULL," +
                    "amount DOUBLE NOT NULL," +
                    "PRIMARY KEY (player_uuid, currency_name)," +
                    "FOREIGN KEY (currency_name) REFERENCES currencies(name) ON DELETE CASCADE)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transaction_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "currency_name VARCHAR(32) NOT NULL," +
                    "amount DOUBLE NOT NULL," +
                    "operation VARCHAR(64) NOT NULL," +
                    "timestamp BIGINT NOT NULL," +
                    "INDEX (player_uuid), INDEX (currency_name))");
        }
    }

    @Override
    public void saveAll() {
        // Для MySQL автосохранение происходит автоматически
    }

    @Override
    public boolean createCurrency(String name, String symbol) {
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
                currencies.put(rs.getString("name"),
                        new Currency(rs.getString("name"), rs.getString("symbol")));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get currencies", e);
        }
        return currencies;
    }

    @Override
    public double getBalance(OfflinePlayer player, String currency) {
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
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO balances (player_uuid, currency_name, amount) " +
                        "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE amount = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, currency);
            stmt.setDouble(3, amount);
            stmt.setDouble(4, amount);
            stmt.executeUpdate();
            logTransaction(player.getUniqueId(), currency, amount, "SET");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to set balance for " + player.getName() + " currency " + currency, e);
        }
    }

    @Override
    public void addBalance(OfflinePlayer player, String currency, double amount) {
        double current = getBalance(player, currency);
        setBalance(player, currency, current + amount);
        logTransaction(player.getUniqueId(), currency, amount, "ADD");
    }

    @Override
    public void removeBalance(OfflinePlayer player, String currency, double amount) {
        double current = getBalance(player, currency);
        setBalance(player, currency, Math.max(0, current - amount));
        logTransaction(player.getUniqueId(), currency, amount, "REMOVE");
    }

    @Override
    public boolean transferBalance(OfflinePlayer from, OfflinePlayer to, String currency, double amount) {
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
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}