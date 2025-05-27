package me.wryuin.database;

import java.util.Objects;

public class TopCacheKey {
    private final String currency;
    private final int limit;
    private final int hashCode;

    public TopCacheKey(String currency, int limit) {
        this.currency = currency != null ? currency : "default";
        this.limit = limit;
        this.hashCode = calculateHashCode();
    }

    public String currency() {
        return currency;
    }

    public int limit() {
        return limit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopCacheKey that = (TopCacheKey) o;
        return limit == that.limit && Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
    
    private int calculateHashCode() {
        return Objects.hash(currency, limit);
    }
} 