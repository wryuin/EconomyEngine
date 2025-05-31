package me.wryuin.security;

import me.wryuin.EconomyEngine;
import me.wryuin.utils.Messages;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SecurityManager {
    private final EconomyEngine plugin;
    private final Map<UUID, Long> lastTransactionTime;
    private final Map<UUID, Integer> transactionCount;
    private final double maxTransactionAmount;
    private final int maxTransactionsPerMinute;
    private final long rateLimitWindowMs;
    private static final long DEFAULT_RATE_LIMIT_WINDOW_MS = 60 * 1000;
    private static final double DEFAULT_MAX_TRANSACTION_AMOUNT = 1_000_000.0;
    private static final int DEFAULT_MAX_TRANSACTIONS_PER_MINUTE = 10;

    public SecurityManager(EconomyEngine plugin) {
        this.plugin = plugin;
        this.lastTransactionTime = new ConcurrentHashMap<>();
        this.transactionCount = new ConcurrentHashMap<>();
        // Load configuration values
        this.maxTransactionAmount = plugin.getConfig().getDouble("security.maxTransactionAmount", DEFAULT_MAX_TRANSACTION_AMOUNT);
        this.maxTransactionsPerMinute = plugin.getConfig().getInt("security.rateLimit.maxTransactionsPerMinute", DEFAULT_MAX_TRANSACTIONS_PER_MINUTE);
        this.rateLimitWindowMs = plugin.getConfig().getLong("security.rateLimit.windowMs", DEFAULT_RATE_LIMIT_WINDOW_MS);
        plugin.getLogger().info("SecurityManager initialized with max transaction amount: " + maxTransactionAmount + ", rate limit: " + maxTransactionsPerMinute + " per " + (rateLimitWindowMs / 1000) + " seconds");
    }

    public boolean validateTransaction(Player player, String currency, double amount) {
        // Check if amount is within limits
        if (amount <= 0 || amount > maxTransactionAmount) {
            player.sendMessage(Messages.get("errors.security.max-amount", maxTransactionAmount));
            plugin.getLogger().log(Level.WARNING, "Transaction rejected for " + player.getName() + ": Amount " + amount + " exceeds limit " + maxTransactionAmount);
            return false;
        }

        // Check rate limit
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastTime = lastTransactionTime.getOrDefault(playerId, 0L);
        int count = transactionCount.getOrDefault(playerId, 0);

        // Reset count if the rate limit window has passed
        if (currentTime - lastTime > rateLimitWindowMs) {
            count = 0;
        }

        // Check if max transactions per window is reached
        if (count >= maxTransactionsPerMinute) {
            player.sendMessage(Messages.get("errors.security.rate-limit", rateLimitWindowMs / 1000));
            plugin.getLogger().log(Level.WARNING, "Transaction rejected for " + player.getName() + ": Rate limit exceeded - " + count + " transactions in " + (rateLimitWindowMs / 1000) + " seconds");
            return false;
        }

        // Update counter and timestamp
        transactionCount.put(playerId, count + 1);
        lastTransactionTime.put(playerId, currentTime);

        // Validate currency exists
        if (!plugin.getDatabase().currencyExists(currency)) {
            player.sendMessage(Messages.get("currency-not-found", currency));
            plugin.getLogger().log(Level.WARNING, "Transaction rejected for " + player.getName() + ": Currency " + currency + " not found");
            return false;
        }

        // Log successful validation
        plugin.getLogger().log(Level.INFO, "Transaction validated for " + player.getName() + ": Amount " + amount + " " + currency);
        return true;
    }

    public boolean validateTransfer(Player from, Player to, String currency, double amount) {
        // Basic validation
        if (!validateTransaction(from, currency, amount)) {
            return false;
        }

        // Check if sender has enough balance
        if (plugin.getDatabase().getBalance(from, currency) < amount) {
            from.sendMessage(Messages.get("errors.insufficient-funds", amount, currency));
            plugin.getLogger().log(Level.WARNING, "Transfer rejected from " + from.getName() + " to " + to.getName() + ": Insufficient funds for " + amount + " " + currency);
            return false;
        }

        // Check if players are the same
        if (from.getUniqueId().equals(to.getUniqueId())) {
            from.sendMessage(Messages.get("errors.security.self-transfer"));
            plugin.getLogger().log(Level.WARNING, "Transfer rejected for " + from.getName() + ": Self-transfer attempt");
            return false;
        }

        // Log successful transfer validation
        plugin.getLogger().log(Level.INFO, "Transfer validated from " + from.getName() + " to " + to.getName() + ": Amount " + amount + " " + currency);
        return true;
    }

    public void clearPlayerLimiter(UUID playerId) {
        lastTransactionTime.remove(playerId);
        transactionCount.remove(playerId);
        plugin.getLogger().log(Level.INFO, "Rate limiter cleared for player ID: " + playerId);
    }

    public void resetAllLimiters() {
        lastTransactionTime.clear();
        transactionCount.clear();
        plugin.getLogger().log(Level.INFO, "All rate limiters reset");
    }

    // Getter methods for configuration values
    public double getMaxTransactionAmount() {
        return maxTransactionAmount;
    }

    public int getMaxTransactionsPerMinute() {
        return maxTransactionsPerMinute;
    }

    public long getRateLimitWindowMs() {
        return rateLimitWindowMs;
    }
} 