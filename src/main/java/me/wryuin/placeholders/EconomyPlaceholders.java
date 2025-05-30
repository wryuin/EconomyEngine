package me.wryuin.placeholders;

import me.wryuin.EconomyEngine;
import me.wryuin.database.DataBase;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.wryuin.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyPlaceholders extends PlaceholderExpansion {
    private final EconomyEngine plugin;
    private final DataBase database;
    
    private final Map<String, Map<UUID, Double>> topPlayersCache = new ConcurrentHashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 300000;

    public EconomyPlaceholders(EconomyEngine plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
    }

    @Override
    public String getIdentifier() {
        return "EconomyEngine";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        
        DataBase db = plugin.getDatabase();
        String[] parts = params.split("_");

        // Handle value placeholders
        if (parts.length >= 2 && parts[0].equalsIgnoreCase("value")) {
            String currency = parts[1];
            if (!db.currencyExists(currency)) {
                return "0";
            }

            double value = db.getBalance(player, currency);
            
            // Handle different formats
            if (parts.length >= 3) {
                String formatType = parts[2];
                switch (formatType.toLowerCase()) {
                    case "fixed":
                        return NumberFormatter.formatWithCommas(value);
                    case "letter":
                        return NumberFormatter.formatToLetter(value);
                    case "formatted":
                        return plugin.getDatabase().getCurrencies().get(currency).formatAmount(value);
                    default:
                        return String.valueOf(value);
                }
            }
            
            return String.valueOf(value);
        }
        
        // Handle top placeholders
        if (parts.length >= 3 && parts[0].equalsIgnoreCase("top")) {
            String currency = parts[1];
            if (!db.currencyExists(currency)) {
                return "Unknown";
            }
            
            try {
                int position = Integer.parseInt(parts[2]);
                if (position < 1 || position > 10) {
                    return "Invalid position";
                }
                
                if (parts.length >= 4) {
                    String type = parts[3].toLowerCase();
                    switch (type) {
                        case "money":
                            return getTopPlayerMoney(currency, position);
                        case "formatted":
                            return getTopPlayerMoneyFormatted(currency, position);
                        default:
                            return getTopPlayerName(currency, position);
                    }
                } else {
                    return getTopPlayerName(currency, position);
                }
            } catch (NumberFormatException e) {
                return "Invalid position";
            }
        }
        
        // Handle currency information
        if (parts.length >= 2 && parts[0].equalsIgnoreCase("currency")) {
            String currency = parts[1];
            if (!db.currencyExists(currency)) {
                return "";
            }
            
            if (parts.length >= 3) {
                String property = parts[2].toLowerCase();
                switch (property) {
                    case "name":
                        return db.getCurrencies().get(currency).getName();
                    case "symbol":
                        return db.getCurrencies().get(currency).getSymbol();
                    case "format":
                        return db.getCurrencies().get(currency).getFormat();
                    default:
                        return "";
                }
            }
        }

        return null;
    }
    
    private String getTopPlayerName(String currency, int position) {
        Map<UUID, Double> topBalances = getTopBalancesFromCache(currency);
        if (topBalances.size() < position) {
            return "N/A";
        }
        
        UUID playerId = topBalances.keySet().toArray(new UUID[0])[position - 1];
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return player.getName() != null ? player.getName() : playerId.toString().substring(0, 8);
    }
    
    private String getTopPlayerMoney(String currency, int position) {
        Map<UUID, Double> topBalances = getTopBalancesFromCache(currency);
        if (topBalances.size() < position) {
            return "0";
        }
        
        Double amount = topBalances.values().toArray(new Double[0])[position - 1];
        return NumberFormatter.formatWithCommas(amount);
    }
    
    private String getTopPlayerMoneyFormatted(String currency, int position) {
        Map<UUID, Double> topBalances = getTopBalancesFromCache(currency);
        if (topBalances.size() < position) {
            return "0";
        }
        
        Double amount = topBalances.values().toArray(new Double[0])[position - 1];
        return plugin.getDatabase().getCurrencies().get(currency).formatAmount(amount);
    }
    
    private Map<UUID, Double> getTopBalancesFromCache(String currency) {
        return plugin.getCacheManager().getTopBalances(currency, 10);
    }
}