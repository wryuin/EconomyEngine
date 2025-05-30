package me.wryuin.commands;

import me.wryuin.EconomyEngine;
import me.wryuin.gui.EconomyGUI;
import me.wryuin.utils.MessageSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GUICommand implements CommandExecutor {
    private final EconomyEngine plugin;
    private final EconomyGUI economyGUI;

    public GUICommand(EconomyEngine plugin, EconomyGUI economyGUI) {
        this.plugin = plugin;
        this.economyGUI = economyGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageSender.sendError(sender, "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("economyengine.gui")) {
            MessageSender.sendError(player, "You don't have permission to use this command.");
            return true;
        }

        economyGUI.openMainMenu(player);
        return true;
    }
}