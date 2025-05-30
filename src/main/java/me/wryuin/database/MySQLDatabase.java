package me.wryuin.database;

import me.wryuin.EconomyEngine;
import me.wryuin.data.PlayerData;
import me.wryuin.Currency;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySQLDatabase implements DataBase {
    private final EconomyEngine plugin;
    private Connection connection;
    private final Queue<Transaction> logQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private DataSource dataSource;
    private static final String INSERT_BATCH_SQL = 
        "INSERT INTO transaction_logs (player_uuid, currency_name, amount, operation, timestamp) VALUES (?, ?, ?, ?, ?)";

    public MySQLDatabase(EconomyEngine plugin) {
        this.plugin = plugin;
        setupDataSource();
        startBatchProcessor();
    }

    private void setupDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + plugin.getConfig().getString("mysql.host") + ":" + 
                         plugin.getConfig().getInt("mysql.port") + "/" + 
                         plugin.getConfig().getString("mysql.database") + "?useSSL=false");
        config.setUsername(plugin.getConfig().getString("mysql.username"));
        config.setPassword(plugin.getConfig().getString("mysql.password"));
        config.setMaximumPoolSize(10);
        
        dataSource = new HikariDataSource(config);
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    @Override
    public void reload() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            initialize();
        } catch (SQLException e) {
            plugin.getLogger().severe("MySQL reload failed: " + e.getMessage());
        }
    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT currency_name, amount FROM balances WHERE player_uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                data.setBalance(
                        rs.getString("currency_name"),
                        rs.getDouble("amount")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load player data: " + e.getMessage());
        }
        return data;
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
        // mySql have auto-save
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
        Transaction transaction = new Transaction(player, currency, amount, operation);
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_BATCH_SQL)) {
            stmt.setString(1, player.toString());
            stmt.setString(2, currency);
            stmt.setDouble(3, amount);
            stmt.setString(4, operation);
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();
            logQueue.add(transaction);
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

    private void startBatchProcessor() {
        scheduler.scheduleAtFixedRate(() -> {
            List<Transaction> batch = new ArrayList<>(100);
            while (batch.size() < 100 && !logQueue.isEmpty()) {
                batch.add(logQueue.poll());
            }

            if (!batch.isEmpty()) {
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(INSERT_BATCH_SQL)) {

                    for (Transaction t : batch) {
                        stmt.setString(1, t.player().toString());
                        stmt.setString(2, t.currency());
                        stmt.setDouble(3, t.amount());
                        stmt.setString(4, t.operation());
                        stmt.setLong(5, t.timestamp());
                        stmt.addBatch();
                    }

                    stmt.executeBatch();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to process transaction batch", e);
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void close() throws Exception {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        
        if (dataSource instanceof AutoCloseable) {
            ((AutoCloseable) dataSource).close();
        }
    }
}