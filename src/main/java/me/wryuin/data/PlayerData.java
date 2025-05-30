package me.wryuin.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final UUID uuid;
    private final Map<String, Double> balances;
    private long lastUpdated;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.balances = new HashMap<>();
        this.lastUpdated = System.currentTimeMillis();
    }

    public UUID getUuid() {
        return uuid;
    }

    public double getBalance(String currency) {
        return balances.getOrDefault(currency, 0.0);
    }

    public void setBalance(String currency, double amount) {
        balances.put(currency, amount);
        this.lastUpdated = System.currentTimeMillis();
    }

    public void addBalance(String currency, double amount) {
        double current = getBalance(currency);
        setBalance(currency, current + amount);
    }

    public boolean removeBalance(String currency, double amount) {
        double current = getBalance(currency);
        if (current >= amount) {
            setBalance(currency, current - amount);
            return true;
        }
        return false;
    }

    public Map<String, Double> getBalances() {
        return new HashMap<>(balances);
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void updateTimestamp() {
        this.lastUpdated = System.currentTimeMillis();
    }
}