package me.wryuin.database;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.wryuin.EconomyEngine;
import me.wryuin.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {
    private final EconomyEngine plugin;
    private final LoadingCache<UUID, PlayerData> playerCache;
    private final Map<TopCacheKey, Map<UUID, Double>> topBalanceCache;
    private final LoadingCache<TopCacheKey, List<TopEntry>> topCache;
    

    public CacheManager(EconomyEngine plugin) {
        this.plugin = plugin;
        this.playerCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build(this::loadFromDatabase);
        this.topBalanceCache = new ConcurrentHashMap<>();
        this.topCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(100)
                .build(this::loadTopFromDB);
    }

    public CompletableFuture<List<TopEntry>> getTop(String currency, int page) {
        return CompletableFuture.supplyAsync(() -> topCache.get(new TopCacheKey(currency, page)));
    }

    private List<TopEntry> loadTopFromDB(TopCacheKey key) {
        List<TopEntry> result = new ArrayList<>();
        Map<UUID, Double> balances = plugin.getDatabase().getTopBalances(key.currency(), key.limit());
        
        if (balances == null || balances.isEmpty()) {
            return result;
        }
        
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            if (entry.getKey() == null) continue;
            
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String name = player.getName() != null ? player.getName() : entry.getKey().toString();
            result.add(new TopEntry(entry.getKey(), name, entry.getValue()));
        }
        
        return result;
    }

    private PlayerData loadFromDatabase(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return plugin.getDatabase().loadPlayerData(uuid);
    }

    public void invalidateAll() {
        playerCache.invalidateAll();
        topCache.invalidateAll();
        topBalanceCache.clear();
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return playerCache.get(uuid);
    }

    public Map<UUID, Double> getTopBalances(String currency, int limit) {
        if (currency == null || currency.isEmpty()) {
            return new ConcurrentHashMap<>();
        }
        
        TopCacheKey key = new TopCacheKey(currency, limit);
        Map<UUID, Double> cached = topBalanceCache.get(key);
        if (cached != null) {
            return cached;
        }

        Map<UUID, Double> fresh = plugin.getDatabase().getTopBalances(currency, limit);
        if (fresh != null) {
            topBalanceCache.put(key, fresh);
        } else {
            fresh = new ConcurrentHashMap<>();
        }
        return fresh;
    }

    public void invalidateTopBalances(String currency) {
        if (currency == null) {
            return;
        }
        topBalanceCache.keySet().removeIf(key -> key.currency().equals(currency));
        topCache.invalidateAll();
    }
}