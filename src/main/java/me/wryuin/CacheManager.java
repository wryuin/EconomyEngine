package me.wryuin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all caching for the plugin
 */
public class CacheManager {
    private final EconomyEngine plugin;
    private final Map<UUID, Map<String, Double>> balanceCache = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, Double>> topBalancesCache = new ConcurrentHashMap<>();
    private long lastTopBalanceUpdate = 0;
    private static final long TOP_BALANCE_CACHE_DURATION = 300000; // 5 minutes

    public CacheManager(EconomyEngine plugin) {
        this.plugin = plugin;
    }

    /**
     * Clears all caches
     */
    public void invalidateAll() {
        balanceCache.clear();
        topBalancesCache.clear();
        lastTopBalanceUpdate = 0;
        
        plugin.getLogger().info("All caches have been invalidated");
    }

    /**
     * Invalidates cache for a specific player
     * 
     * @param uuid Player UUID
     */
    public void invalidatePlayer(UUID uuid) {
        balanceCache.remove(uuid);
    }

    /**
     * Invalidates top balances cache for a specific currency
     * 
     * @param currency Currency ID
     */
    public void invalidateTopBalances(String currency) {
        topBalancesCache.remove(currency.toLowerCase());
    }

    /**
     * Gets cached top balances for a currency, refreshing if needed
     * 
     * @param currency Currency ID
     * @param limit Number of top players to fetch
     * @return Map of UUIDs to balances
     */
    public Map<UUID, Double> getTopBalances(String currency, int limit) {
        long currentTime = System.currentTimeMillis();
        
        // If cache is expired or doesn't exist, refresh it
        if (currentTime - lastTopBalanceUpdate > TOP_BALANCE_CACHE_DURATION ||
                !topBalancesCache.containsKey(currency.toLowerCase())) {
            Map<UUID, Double> fresh = plugin.getDatabase().getTopBalances(currency, limit);
            topBalancesCache.put(currency.toLowerCase(), fresh);
            lastTopBalanceUpdate = currentTime;
        }
        
        return topBalancesCache.getOrDefault(currency.toLowerCase(), new HashMap<>());
    }

    /**
     * Caches a player's balance for a specific currency
     * 
     * @param uuid Player UUID
     * @param currency Currency ID
     * @param balance Balance amount
     */
    public void cacheBalance(UUID uuid, String currency, double balance) {
        balanceCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                   .put(currency.toLowerCase(), balance);
    }

    /**
     * Gets a cached balance or null if not cached
     * 
     * @param uuid Player UUID
     * @param currency Currency ID
     * @return Cached balance or null
     */
    public Double getCachedBalance(UUID uuid, String currency) {
        Map<String, Double> playerCache = balanceCache.get(uuid);
        if (playerCache == null) {
            return null;
        }
        return playerCache.get(currency.toLowerCase());
    }
} 