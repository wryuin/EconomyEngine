package me.wryuin.database;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.wryuin.EconomyEngine;
import me.wryuin.data.PlayerData;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CacheManager {
    private final EconomyEngine plugin;
    private final LoadingCache<UUID, PlayerData> playerCache;

    public CacheManager(EconomyEngine plugin) {
        this.plugin = plugin;
        this.playerCache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build(this::loadFromDatabase);
    }

    private PlayerData loadFromDatabase(UUID uuid) {
        return plugin.getDatabase().loadPlayerData(uuid);
    }

    public void invalidateAll() {
        playerCache.invalidateAll();
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerCache.get(uuid);
    }
}