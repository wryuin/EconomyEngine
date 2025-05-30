package me.wryuin;

import java.io.Serializable;
import java.util.Objects;
import org.bukkit.ChatColor;

public class Currency implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String id;
    private final String name;
    private final String symbol;
    private String format;
    private double defaultAmount;
    private double maxAmount;
    private boolean transferable;
    private ChatColor color;

    /**
     * Creates a basic currency with default settings
     * 
     * @param id Unique identifier for the currency
     * @param name Display name of the currency
     * @param symbol Currency symbol
     */
    public Currency(String id, String name, String symbol) {
        this.id = id.toLowerCase();
        this.name = name;
        this.symbol = symbol;
        this.format = "%s%,.2f %s";
        this.defaultAmount = 0.0;
        this.maxAmount = Double.MAX_VALUE;
        this.transferable = true;
        this.color = ChatColor.WHITE;
    }

    /**
     * Creates a currency with all custom settings
     * 
     * @param id Unique identifier for the currency
     * @param name Display name of the currency
     * @param symbol Currency symbol
     * @param format Format string for displaying amounts
     * @param defaultAmount Default starting amount
     * @param maxAmount Maximum possible amount
     * @param transferable Whether this currency can be transferred between players
     * @param color Chat color for display
     */
    public Currency(String id, String name, String symbol, String format, 
                   double defaultAmount, double maxAmount, boolean transferable, 
                   ChatColor color) {
        this.id = id.toLowerCase();
        this.name = name;
        this.symbol = symbol;
        this.format = format;
        this.defaultAmount = defaultAmount;
        this.maxAmount = maxAmount;
        this.transferable = transferable;
        this.color = color;
    }

    /**
     * Gets the unique ID of this currency
     * 
     * @return Currency ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the display name of this currency
     * 
     * @return Currency name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the symbol for this currency
     * 
     * @return Currency symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Gets the format string for displaying amounts
     * 
     * @return Format string
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets the format string for displaying amounts
     * 
     * @param format New format string
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Gets the default starting amount for new players
     * 
     * @return Default amount
     */
    public double getDefaultAmount() {
        return defaultAmount;
    }

    /**
     * Sets the default starting amount for new players
     * 
     * @param defaultAmount New default amount
     */
    public void setDefaultAmount(double defaultAmount) {
        this.defaultAmount = defaultAmount;
    }

    /**
     * Gets the maximum allowed amount
     * 
     * @return Maximum amount
     */
    public double getMaxAmount() {
        return maxAmount;
    }

    /**
     * Sets the maximum allowed amount
     * 
     * @param maxAmount New maximum amount
     */
    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }

    /**
     * Checks if the currency can be transferred between players
     * 
     * @return true if transferable, false otherwise
     */
    public boolean isTransferable() {
        return transferable;
    }

    /**
     * Sets whether this currency can be transferred between players
     * 
     * @param transferable New transferable state
     */
    public void setTransferable(boolean transferable) {
        this.transferable = transferable;
    }

    /**
     * Gets the display color for this currency
     * 
     * @return Chat color
     */
    public ChatColor getColor() {
        return color;
    }

    /**
     * Sets the display color for this currency
     * 
     * @param color New chat color
     */
    public void setColor(ChatColor color) {
        this.color = color;
    }

    /**
     * Formats an amount of this currency according to the format string
     * 
     * @param amount Amount to format
     * @return Formatted currency string
     */
    public String formatAmount(double amount) {
        return String.format(format, color, amount, symbol);
    }

    @Override
    public String toString() {
        return color + name + " (" + symbol + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Currency currency = (Currency) o;
        return Objects.equals(id, currency.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
