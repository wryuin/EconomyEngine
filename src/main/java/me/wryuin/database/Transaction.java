package me.wryuin.database;

import java.util.UUID;

public class Transaction {
    private final UUID player;
    private final String currency;
    private final double amount;
    private final String operation;
    private final long timestamp;

    public Transaction(UUID player, String currency, double amount, String operation) {
        this(player, currency, amount, operation, System.currentTimeMillis());
    }

    public Transaction(UUID player, String currency, double amount, String operation, long timestamp) {
        this.player = player;
        this.currency = currency;
        this.amount = amount;
        this.operation = operation;
        this.timestamp = timestamp;
    }

    public UUID player() {
        return player;
    }

    public String currency() {
        return currency;
    }

    public double amount() {
        return amount;
    }

    public String operation() {
        return operation;
    }

    public long timestamp() {
        return timestamp;
    }
} 