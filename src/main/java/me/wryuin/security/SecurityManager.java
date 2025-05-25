package me.wryuin.security;

import me.wryuin.EconomyEngine;
import me.wryuin.utils.Messages;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityManager {
    private final EconomyEngine plugin;
    private final Map<UUID, Long> lastTransactionTime;
    private final Map<UUID, Integer> transactionCount;
    private static final double MAX_TRANSACTION_AMOUNT = 1_000_000.0;
    private static final int MAX_TRANSACTIONS_PER_MINUTE = 10;
    private static final long ONE_MINUTE_MS = 60 * 1000;

    public SecurityManager(EconomyEngine plugin) {
        this.plugin = plugin;
        this.lastTransactionTime = new ConcurrentHashMap<>();
        this.transactionCount = new ConcurrentHashMap<>();
    }

    public boolean validateTransaction(Player player, String currency, double amount) {
        // Check if amount is within limits
        if (amount <= 0 || amount > MAX_TRANSACTION_AMOUNT) {
            player.sendMessage(Messages.get("errors.security.max-amount", MAX_TRANSACTION_AMOUNT));
            return false;
        }

        // Check rate limit
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastTime = lastTransactionTime.getOrDefault(playerId, 0L);
        int count = transactionCount.getOrDefault(playerId, 0);

        // Reset count if more than a minute has passed
        if (currentTime - lastTime > ONE_MINUTE_MS) {
            count = 0;
        }

        // Check if max transactions per minute is reached
        if (count >= MAX_TRANSACTIONS_PER_MINUTE) {
            player.sendMessage(Messages.get("errors.security.rate-limit", 60));
            return false;
        }

        // Update counter and timestamp
        transactionCount.put(playerId, count + 1);
        lastTransactionTime.put(playerId, currentTime);

        // Validate currency exists
        if (!plugin.getDatabase().currencyExists(currency)) {
            player.sendMessage(Messages.get("currency-not-found", currency));
            return false;
        }

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
            return false;
        }

        // Check if players are the same
        if (from.getUniqueId().equals(to.getUniqueId())) {
            from.sendMessage(Messages.get("errors.security.self-transfer"));
            return false;
        }

        return true;
    }

    public void clearPlayerLimiter(UUID playerId) {
        lastTransactionTime.remove(playerId);
        transactionCount.remove(playerId);
    }

    public void resetAllLimiters() {
        lastTransactionTime.clear();
        transactionCount.clear();
    }
} 