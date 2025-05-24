package me.wryuin.utils;

import me.wryuin.EconomyEngine;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;

public class Messages {
    private static YamlConfiguration config;
    private static EconomyEngine plugin;

    public static void init(EconomyEngine plugin) {
        Messages.plugin = plugin;
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static String get(String path, Object... args) {
        String message = config.getString(path, "&cСообщение не найдено: " + path);
        message = ColorUtils.colorize(message.replace("{prefix}", getPrefix()));

        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", args[i].toString());
        }
        return message;
    }

    private static String getPrefix() {
        return config.getString("prefix", "");
    }

    public static String[] getList(String path, Object... args) {
        return Arrays.stream(config.getStringList(path).toArray(new String[0]))
                .map(line -> get(path + "." + line, args))
                .toArray(String[]::new);
    }
}