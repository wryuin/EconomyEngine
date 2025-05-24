package me.wryuin.placeholders;

import me.wryuin.EconomyEngine;
import me.wryuin.database.DataBase;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.wryuin.utils.NumberFormatter;
import org.bukkit.OfflinePlayer;

public class EconomyPlaceholders extends PlaceholderExpansion {
    private final EconomyEngine plugin;
    private final DataBase database;

    public EconomyPlaceholders(EconomyEngine plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
    }

    @Override
    public String getIdentifier() {
        return "EconomyEngine";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        DataBase db = plugin.getDatabase();
        String[] parts = params.split("_");

        if (parts.length < 3 || !parts[0].equalsIgnoreCase("value")) {
            return null;
        }

        String currency = parts[1];
        if (!db.currencyExists(currency)) {
            return "0";
        }

        double value = db.getBalance(player, currency);
        String formatType = parts[2];

        switch (formatType.toLowerCase()) {
            case "fixed":
                return NumberFormatter.formatWithCommas(value);
            case "letter":
                return NumberFormatter.formatToLetter(value);
            default:
                return String.valueOf(value);
        }
    }
}