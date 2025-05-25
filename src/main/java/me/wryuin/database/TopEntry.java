package me.wryuin.database;

import java.util.UUID;

public class TopEntry {
    private final UUID playerId;
    private final String playerName;
    private final double amount;

    public TopEntry(UUID playerId, String playerName, double amount) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.amount = amount;
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
               playerId.equals(topEntry.playerId) &&
               playerName.equals(topEntry.playerName);
    }

    @Override
    public int hashCode() {
        int result = playerId.hashCode();
        result = 31 * result + playerName.hashCode();
        long temp = Double.doubleToLongBits(amount);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
} 