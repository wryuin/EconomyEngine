package me.wryuin.utils;

import org.bukkit.command.CommandSender;

/**
 * Utility class for sending formatted messages to players and command senders.
 */
public class MessageSender {
    
    /**
     * Sends a formatted message to a command sender.
     * 
     * @param sender The recipient of the message
     * @param message The message to send (can contain color codes)
     */
    public static void send(CommandSender sender, String message) {
        if (sender == null) return;
        sender.sendMessage(ColorUtils.colorize(message));
    }
    
    /**
     * Sends a message from the messages.yml file to a command sender.
     * 
     * @param sender The recipient of the message
     * @param key The message key in messages.yml
     * @param args Optional arguments for message placeholders
     */
    public static void sendMessage(CommandSender sender, String key, Object... args) {
        if (sender == null) return;
        sender.sendMessage(Messages.get(key, args));
    }
    
    /**
     * Sends an error message to a command sender.
     * 
     * @param sender The recipient of the message
     * @param message The error message
     */
    public static void sendError(CommandSender sender, String message) {
        if (sender == null) return;
        sender.sendMessage(ColorUtils.colorize("&c" + message));
    }
}