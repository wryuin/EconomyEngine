package me.wryuin.messaging;

import java.io.Serializable;
import java.util.UUID;

/**
 * Message used for transactions between servers
 */
public class TransactionMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final UUID from;
    private final UUID to;
    private final String currency;
    private final double amount;
    
    public TransactionMessage(UUID from, UUID to, String currency, double amount) {
        this.from = from;
        this.to = to;
        this.currency = currency;
        this.amount = amount;
    }
    
    public UUID getFrom() {
        return from;
    }
    
    public UUID getTo() {
        return to;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public double getAmount() {
        return amount;
    }
} 