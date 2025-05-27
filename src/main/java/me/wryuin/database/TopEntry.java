package me.wryuin.database;

import java.util.Objects;
import java.util.UUID;

public class TopEntry {
    private final UUID playerId;
    private final String playerName;
    private final double amount;
    private final int hashCode;

    public TopEntry(UUID playerId, String playerName, double amount) {
        this.playerId = playerId;
        this.playerName = playerName != null ? playerName : "Unknown";
        this.amount = amount;
        this.hashCode = calculateHashCode();
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
    }

    public double amount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopEntry topEntry = (TopEntry) o;
        return Double.compare(topEntry.amount, amount) == 0 &&
               Objects.equals(playerId, topEntry.playerId) &&
               Objects.equals(playerName, topEntry.playerName);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
    
    private int calculateHashCode() {
        return Objects.hash(playerId, playerName, amount);
    }
} 