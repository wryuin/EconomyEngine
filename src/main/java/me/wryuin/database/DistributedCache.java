package me.wryuin.database;

import com.google.gson.Gson;
import me.wryuin.EconomyEngine;
import me.wryuin.data.PlayerData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

public class DistributedCache {
    private final EconomyEngine plugin;
    private final JedisPool jedisPool;
    private final Gson gson;
    private static final String PLAYER_KEY_PREFIX = "player:";
    private static final String TOP_KEY_PREFIX = "top:";
    private static final int CACHE_TTL = 300; // 5 minutes

    public DistributedCache(EconomyEngine plugin) {
        this.plugin = plugin;
        this.gson = new Gson();

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(0);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setMinEvictableIdleTime(Duration.ofMinutes(1));
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);

        String host = plugin.getConfig().getString("redis.host", "localhost");
        int port = plugin.getConfig().getInt("redis.port", 6379);
        String password = plugin.getConfig().getString("redis.password", null);

        if (password != null && password.isEmpty()) {
            password = null;
        }

        this.jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
    }

    public void setPlayerData(UUID uuid, PlayerData data) {
        String key = PLAYER_KEY_PREFIX + uuid.toString();
        String json = gson.toJson(data);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, CACHE_TTL, json);
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        String key = PLAYER_KEY_PREFIX + uuid.toString();
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(key);
            if (json != null) {
                return gson.fromJson(json, PlayerData.class);
            }
        }
        return null;
    }

    public void setTopBalances(String currency, Map<UUID, Double> balances) {
        String key = TOP_KEY_PREFIX + currency;
        String json = gson.toJson(balances);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, CACHE_TTL, json);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<UUID, Double> getTopBalances(String currency) {
        String key = TOP_KEY_PREFIX + currency;
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(key);
            if (json != null) {
                return gson.fromJson(json, Map.class);
            }
        }
        return null;
    }

    public void invalidatePlayerData(UUID uuid) {
        String key = PLAYER_KEY_PREFIX + uuid.toString();
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        }
    }

    public void invalidateTopBalances(String currency) {
        String key = TOP_KEY_PREFIX + currency;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        }
    }

    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
} 