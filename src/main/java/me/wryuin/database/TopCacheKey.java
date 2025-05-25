package me.wryuin.database;

public class TopCacheKey {
    private final String currency;
    private final int limit;

    public TopCacheKey(String currency, int limit) {
        this.currency = currency;
        this.limit = limit;
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
        return limit == that.limit && currency.equals(that.currency);
    }

    @Override
    public int hashCode() {
        return 31 * currency.hashCode() + limit;
    }
} 