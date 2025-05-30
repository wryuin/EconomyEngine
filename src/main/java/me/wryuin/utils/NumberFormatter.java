package me.wryuin.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumberFormatter {
    private static final DecimalFormat COMMA_FORMAT = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat PLAIN_FORMAT = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));

    /**
     * Formats a number with commas as thousand separators
     * 
     * @param number The number to format
     * @return The formatted string
     */
    public static String formatWithCommas(double number) {
        return COMMA_FORMAT.format(number);
    }

    /**
     * Formats a large number with letter suffixes (K, M, B, T)
     * 
     * @param number The number to format
     * @return The formatted string with suffix
     */
    public static String formatToLetter(double number) {
        if (number < 1000) return COMMA_FORMAT.format(number);

        int exp = (int) (Math.log(number) / Math.log(1000));
        char suffix = "KMBT".charAt(exp - 1);

        return String.format("%.1f%c", number / Math.pow(1000, exp), suffix);
    }
    
    /**
     * Formats a number with a fixed number of decimal places
     * 
     * @param number The number to format
     * @param decimals The number of decimal places
     * @return The formatted string
     */
    public static String formatFixed(double number, int decimals) {
        if (decimals < 0) decimals = 0;
        
        StringBuilder pattern = new StringBuilder("0");
        if (decimals > 0) {
            pattern.append(".");
            for (int i = 0; i < decimals; i++) {
                pattern.append("0");
            }
        }
        
        DecimalFormat df = new DecimalFormat(pattern.toString(), new DecimalFormatSymbols(Locale.US));
        return df.format(number);
    }
}