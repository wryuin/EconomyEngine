package me.wryuin.database;

import me.wryuin.Currency;
import me.wryuin.EconomyEngine;
import me.wryuin.data.PlayerData;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;

public class DatabaseManager {
    private final EconomyEngine plugin;
    private final DataBase primaryDatabase;
    private final DataBase backupDatabase;
    private boolean isPrimaryActive = true;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second

    public DatabaseManager(EconomyEngine plugin, DataBase primary, DataBase backup) {
        this.plugin = plugin;
        this.primaryDatabase = primary;
        this.backupDatabase = backup;
    }

    private <T> T executeWithFailover(final DatabaseCallback<T> callback) {
        Exception lastException = null;
        
        // Try primary database first
        if (isPrimaryActive) {
            for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
                try {
                    return callback.execute(primaryDatabase);
                } catch (Exception e) {
                    lastException = e;
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            // If all attempts failed, switch to backup
            plugin.getLogger().warning("Primary database failed after " + MAX_RETRY_ATTEMPTS + 
                    " attempts, switching to backup: " + 
                    (lastException != null ? lastException.getMessage() : "Unknown error"));
            isPrimaryActive = false;
        }

        // Try backup database
        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return callback.execute(backupDatabase);
            } catch (Exception e) {
                lastException = e;
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // If both databases failed
        String errorMsg = "Both databases failed after multiple attempts";
        if (lastException != null) {
            errorMsg += ": " + lastException.getMessage();
        }
        plugin.getLogger().severe(errorMsg);
        throw new RuntimeException("Database operation failed", lastException);
    }

    public void initialize() {
        executeWithFailover(new DatabaseCallback<Void>() {
            @Override
            public Void execute(DataBase db) {
                db.initialize();
                return null;
            }
        });
    }

    public void saveAll() {
        executeWithFailover(new DatabaseCallback<Void>() {
            @Override
            public Void execute(DataBase db) {
                db.saveAll();
                return null;
            }
        });
    }

    public void reload() {
        executeWithFailover(new DatabaseCallback<Void>() {
            @Override
            public Void execute(DataBase db) {
                db.reload();
                return null;
            }
        });
    }

    public boolean createCurrency(final String name, final String symbol) {
        return executeWithFailover(new DatabaseCallback<Boolean>() {
            @Override
            public Boolean execute(DataBase db) {
                return db.createCurrency(name, symbol);
            }
        });
    }

    public boolean deleteCurrency(final String name) {
        return executeWithFailover(new DatabaseCallback<Boolean>() {
            @Override
            public Boolean execute(DataBase db) {
                return db.deleteCurrency(name);
            }
        });
    }

    public boolean currencyExists(final String name) {
        return executeWithFailover(new DatabaseCallback<Boolean>() {
            @Override
            public Boolean execute(DataBase db) {
                return db.currencyExists(name);
            }
        });
    }

    public Map<String, Currency> getCurrencies() {
        return executeWithFailover(new DatabaseCallback<Map<String, Currency>>() {
            @Override
            public Map<String, Currency> execute(DataBase db) {
                return db.getCurrencies();
            }
        });
    }

    public PlayerData loadPlayerData(final UUID uuid) {
        return executeWithFailover(new DatabaseCallback<PlayerData>() {
            @Override
            public PlayerData execute(DataBase db) {
                return db.loadPlayerData(uuid);
            }
        });
    }

    public double getBalance(final OfflinePlayer player, final String currency) {
        return executeWithFailover(new DatabaseCallback<Double>() {
            @Override
            public Double execute(DataBase db) {
                return db.getBalance(player, currency);
            }
        });
    }

    public void setBalance(final OfflinePlayer player, final String currency, final double amount) {
        executeWithFailover(new DatabaseCallback<Void>() {
            @Override
            public Void execute(DataBase db) {
                db.setBalance(player, currency, amount);
                return null;
            }
        });
    }

    public void addBalance(final OfflinePlayer player, final String currency, final double amount) {
        executeWithFailover(new DatabaseCallback<Void>() {
            @Override
            public Void execute(DataBase db) {
                db.addBalance(player, currency, amount);
                return null;
            }
        });
    }

    public void removeBalance(final OfflinePlayer player, final String currency, final double amount) {
        executeWithFailover(new DatabaseCallback<Void>() {
            @Override
            public Void execute(DataBase db) {
                db.removeBalance(player, currency, amount);
                return null;
            }
        });
    }

    public boolean transferBalance(final OfflinePlayer from, final OfflinePlayer to, final String currency, final double amount) {
        return executeWithFailover(new DatabaseCallback<Boolean>() {
            @Override
            public Boolean execute(DataBase db) {
                return db.transferBalance(from, to, currency, amount);
            }
        });
    }

    public void logTransaction(final UUID player, final String currency, final double amount, final String operation) {
        executeWithFailover(new DatabaseCallback<Void>() {
            @Override
            public Void execute(DataBase db) {
                db.logTransaction(player, currency, amount, operation);
                return null;
            }
        });
    }

    public Map<UUID, Double> getTopBalances(final String currency, final int limit) {
        return executeWithFailover(new DatabaseCallback<Map<UUID, Double>>() {
            @Override
            public Map<UUID, Double> execute(DataBase db) {
                return db.getTopBalances(currency, limit);
            }
        });
    }

    public void close() throws Exception {
        try {
            primaryDatabase.close();
        } finally {
            if (backupDatabase != null) {
                backupDatabase.close();
            }
        }
    }

    public interface DatabaseCallback<T> {
        T execute(DataBase database);
    }
} 