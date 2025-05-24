package me.wryuin.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumberFormatter {
    private static final DecimalFormat COMMA_FORMAT = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.US));

    public static String formatWithCommas(double number) {
        return COMMA_FORMAT.format(number);
    }

    public static String formatToLetter(double number) {
        if (number < 1000) return COMMA_FORMAT.format(number);

        int exp = (int) (Math.log(number) / Math.log(1000));
        char suffix = "KMBT".charAt(exp - 1);

        return String.format("%.1f%c", number / Math.pow(1000, exp), suffix);
    }
}