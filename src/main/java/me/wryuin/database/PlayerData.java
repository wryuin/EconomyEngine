package me.wryuin.database;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private final Map<String, Double> balances;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.balances = new HashMap<>();
    }

    public double getBalance(String currency) {
        return balances.getOrDefault(currency, 0.0);
    }

    public void setBalance(String currency, double amount) {
        balances.put(currency, amount);
    }
}