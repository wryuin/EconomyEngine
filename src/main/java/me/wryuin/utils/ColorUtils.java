package me.wryuin.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final boolean IS_NEW_VERSION = Bukkit.getVersion().contains("1.16") ||
            Bukkit.getVersion().contains("1.17") ||
            Bukkit.getVersion().contains("1.18") ||
            Bukkit.getVersion().contains("1.19");

    public static String colorize(String text) {
        if (!IS_NEW_VERSION) {
            return ChatColor.translateAlternateColorCodes('&', text);
        }

        text = parseGradients(text);
        text = parseHexAndRGB(text);
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String gradient(String start, String end, String text) {
        if (!IS_NEW_VERSION) {
            return text;
        }

        Color from = hexToColor(start);
        Color to = hexToColor(end);
        return applyGradient(from, to, text);
    }

    private static String parseGradients(String text) {
        Pattern pattern = Pattern.compile("\\{gradient\\((.*?),(.*?)\\)\\}(.*?)\\{/gradient\\}");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String startColor = matcher.group(1);
            String endColor = matcher.group(2);
            String content = matcher.group(3);
            text = text.replace(matcher.group(), gradient(startColor, endColor, content));
        }
        return text;
    }

    private static String parseHexAndRGB(String text) {
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        while (hexMatcher.find()) {
            String color = hexMatcher.group(1);
            text = text.replace("&#" + color, ChatColor.of("#" + color).toString());
        }

        Pattern rgbPattern = Pattern.compile("\\{rgb\\((\\d+),(\\d+),(\\d+)\\)\\}");
        Matcher rgbMatcher = rgbPattern.matcher(text);
        while (rgbMatcher.find()) {
            int r = Integer.parseInt(rgbMatcher.group(1));
            int g = Integer.parseInt(rgbMatcher.group(2));
            int b = Integer.parseInt(rgbMatcher.group(3));
            text = text.replace(rgbMatcher.group(), ChatColor.of(new Color(r, g, b)).toString());
        }

        return text;
    }

    private static Color hexToColor(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Invalid HEX format");
        }
        return new Color(
                Integer.valueOf(hex.substring(0, 2), 16),
                Integer.valueOf(hex.substring(2, 4), 16),
                Integer.valueOf(hex.substring(4, 6), 16)
        );
    }

    private static String applyGradient(Color start, Color end, String text) {
        List<Color> colors = interpolateColors(start, end, text.length());
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            result.append(ChatColor.of(colors.get(i))).append(c);
        }
        return result.toString();
    }

    private static List<Color> interpolateColors(Color start, Color end, int steps) {
        List<Color> colors = new ArrayList<>();
        double step = 1.0 / (steps - 1);

        for (int i = 0; i < steps; i++) {
            double ratio = step * i;
            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
            colors.add(new Color(red, green, blue));
        }
        return colors;
    }

    /**
     * Преобразует список строк с цветовыми кодами
     */
    public static List<String> colorize(List<String> list) {
        list.replaceAll(ColorUtils::colorize);
        return list;
    }
}