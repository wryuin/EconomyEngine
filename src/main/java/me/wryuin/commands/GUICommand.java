package me.wryuin.commands;

import me.wryuin.EconomyEngine;
import me.wryuin.gui.EconomyGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GUICommand implements CommandExecutor {
    private final EconomyEngine plugin;
    private final EconomyGUI gui;

    public GUICommand(EconomyEngine plugin, EconomyGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("economyengine.gui")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        gui.openMainMenu(player);
        return true;
    }
}