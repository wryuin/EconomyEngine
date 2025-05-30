package me.wryuin.commands;

import me.wryuin.EconomyEngine;
import me.wryuin.utils.MessageSender;
import me.wryuin.utils.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ReloadCommand {

    public static boolean execute(EconomyEngine plugin, CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length < 2) {
            MessageSender.sendMessage(sender, "commands.economy.reload.types");
            return true;
        }

        String type = args[1].toLowerCase();
        switch(type) {
            case "all":
                reloadConfigs(plugin);
                reloadData(plugin);
                MessageSender.sendMessage(sender, "commands.economy.reload.success", "all");
                break;

            case "config":
                reloadConfigs(plugin);
                MessageSender.sendMessage(sender, "commands.economy.reload.success", "config");
                break;

            case "data":
                reloadData(plugin);
                MessageSender.sendMessage(sender, "commands.economy.reload.success", "data");
                break;

            default:
                MessageSender.sendMessage(sender, "commands.economy.reload.error", type);
                return true;
        }
        return true;
    }

    private static void reloadConfigs(EconomyEngine plugin) {
        plugin.getDatabase().saveAll();
        plugin.reloadConfig();
        plugin.getConfigManager().onConfigReload();
        if (plugin.getEconomyGUI() != null) {
            plugin.getEconomyGUI().reloadConfig();
        }
    }

    private static void reloadData(EconomyEngine plugin) {
        plugin.getDatabase().saveAll();
        plugin.getDatabase().reload();
        plugin.getCacheManager().invalidateAll();
    }
}