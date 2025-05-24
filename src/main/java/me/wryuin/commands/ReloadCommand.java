package me.wryuin.commands;

import me.wryuin.EconomyEngine;
import me.wryuin.utils.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ReloadCommand {

    public static boolean execute(EconomyEngine plugin, CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length < 2) {
            sender.sendMessage(Messages.get("commands.economy.reload.types"));
            return true;
        }

        String type = args[1].toLowerCase();
        switch(type) {
            case "all":
                reloadConfigs(plugin);
                reloadData(plugin);
                sender.sendMessage(Messages.get("commands.economy.reload.success", "all"));
                break;

            case "config":
                reloadConfigs(plugin);
                sender.sendMessage(Messages.get("commands.economy.reload.success", "config"));
                break;

            case "data":
                reloadData(plugin);
                sender.sendMessage(Messages.get("commands.economy.reload.success", "data"));
                break;

            default:
                sender.sendMessage(Messages.get("commands.economy.reload.error", type));
                return true;
        }
        return true;
    }

    private static void reloadConfigs(EconomyEngine plugin) {
        plugin.getConfigManager().setup();
    }

    private static void reloadData(EconomyEngine plugin) {
        plugin.getDatabase().reload();
        plugin.getCacheManager().invalidateAll();
    }
}