package me.wryuin.commands;

import me.wryuin.EconomyEngine;
import me.wryuin.database.DataBase;
import me.wryuin.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class EconomyCommand implements CommandExecutor {
    private final EconomyEngine plugin;

    public EconomyCommand(EconomyEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length == 0) {
            sendHelp(sender);
            return true;
        }

        DataBase db = plugin.getDatabase();

        switch (args[0].toLowerCase()) {
            case "reload":
                if(!sender.hasPermission("economyengine.reload")) {
                    sender.sendMessage(Messages.get("no-permission"));
                    return true;
                }
                return ReloadCommand.execute(plugin, sender, cmd, label, args);

            case "create":
                if (!sender.hasPermission("economyengine.create")) {
                    sender.sendMessage(Messages.get("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Messages.get("commands.economy.usage.create"));
                    return true;
                }
                String currencyName = args[1];
                String symbol = args.length > 2 ? args[2] : "$";
                if (db.createCurrency(currencyName, symbol)) {
                    sender.sendMessage(Messages.get("commands.economy.created", currencyName));
                } else {
                    sender.sendMessage(Messages.get("currency-not-found", currencyName));
                }
                return true;

            case "set":
                if (!sender.hasPermission("economyengine.set")) {
                    sender.sendMessage(Messages.get("no-permission"));
                    return true;
                }
                return handleSetCommand(sender, args, db);

            case "add":
                if (!sender.hasPermission("economyengine.add")) {
                    sender.sendMessage(Messages.get("no-permission"));
                    return true;
                }
                return handleAddRemoveCommand(sender, args, db, true);

            case "remove":
                if (!sender.hasPermission("economyengine.remove")) {
                    sender.sendMessage(Messages.get("no-permission"));
                    return true;
                }
                return handleAddRemoveCommand(sender, args, db, false);

            case "give":
                if (!sender.hasPermission("economyengine.give") || !(sender instanceof Player)) {
                    sender.sendMessage(Messages.get("no-permission"));
                    return true;
                }
                return handleGiveCommand((Player) sender, args, db);

            case "top":
                if (!sender.hasPermission("economyengine.top")) {
                    sender.sendMessage(Messages.get("no-permission"));
                    return true;
                }
                return handleTopCommand(sender, args, db);

            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleSetCommand(CommandSender sender, String[] args, DataBase db) {
        if (args.length < 4) {
            sender.sendMessage(Messages.get("commands.economy.usage.set"));
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            String currency = args[2];
            String playerName = args[3];
            OfflinePlayer target = null;
            
            // Try to find player by name
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().equalsIgnoreCase(playerName)) {
                    target = p;
                    break;
                }
            }
            
            // If not found online, try by UUID or look up offline player
            if (target == null) {
                try {
                    UUID uuid = UUID.fromString(playerName);
                    target = Bukkit.getOfflinePlayer(uuid);
                } catch (IllegalArgumentException ignored) {
                    // Not a UUID, look up by name
                    target = Bukkit.getOfflinePlayer(playerName);
                }
            }

            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                sender.sendMessage(Messages.get("player-not-found", playerName));
                return true;
            }

            if (!db.currencyExists(currency)) {
                sender.sendMessage(Messages.get("currency-not-found", currency));
                return true;
            }

            db.setBalance(target, currency, amount);
            sender.sendMessage(Messages.get("commands.economy.set", target.getName(), currency, amount));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Messages.get("errors.invalid-amount", args[1]));
            return true;
        }
    }

    private boolean handleAddRemoveCommand(CommandSender sender, String[] args, DataBase db, boolean isAdd) {
        if (args.length < 4) {
            sender.sendMessage(Messages.get("commands.economy.usage." + (isAdd ? "add" : "remove")));
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            String currency = args[2];
            String playerName = args[3];
            OfflinePlayer target = null;
            
            // Try to find player by name
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().equalsIgnoreCase(playerName)) {
                    target = p;
                    break;
                }
            }
            
            // If not found online, try by UUID or look up offline player
            if (target == null) {
                try {
                    UUID uuid = UUID.fromString(playerName);
                    target = Bukkit.getOfflinePlayer(uuid);
                } catch (IllegalArgumentException ignored) {
                    // Not a UUID, look up by name
                    target = Bukkit.getOfflinePlayer(playerName);
                }
            }

            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                sender.sendMessage(Messages.get("player-not-found", playerName));
                return true;
            }

            if (!db.currencyExists(currency)) {
                sender.sendMessage(Messages.get("currency-not-found", currency));
                return true;
            }

            if (isAdd) {
                db.addBalance(target, currency, amount);
                sender.sendMessage(Messages.get("commands.economy.add", amount, currency, target.getName()));
            } else {
                db.removeBalance(target, currency, amount);
                sender.sendMessage(Messages.get("commands.economy.remove", amount, currency, target.getName()));
            }
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Messages.get("errors.invalid-amount", args[1]));
            return true;
        }
    }

    private boolean handleGiveCommand(Player sender, String[] args, DataBase db) {
        if (args.length < 4) {
            sender.sendMessage(Messages.get("commands.economy.usage.give"));
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            String currency = args[2];
            String playerName = args[3];
            OfflinePlayer target = null;
            
            // Try to find player by name
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().equalsIgnoreCase(playerName)) {
                    target = p;
                    break;
                }
            }
            
            // If not found online, try by UUID or look up offline player
            if (target == null) {
                try {
                    UUID uuid = UUID.fromString(playerName);
                    target = Bukkit.getOfflinePlayer(uuid);
                } catch (IllegalArgumentException ignored) {
                    // Not a UUID, look up by name
                    target = Bukkit.getOfflinePlayer(playerName);
                }
            }

            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                sender.sendMessage(Messages.get("player-not-found", playerName));
                return true;
            }

            if (!db.currencyExists(currency)) {
                sender.sendMessage(Messages.get("currency-not-found", currency));
                return true;
            }

            if (db.transferBalance(sender, target, currency, amount)) {
                sender.sendMessage(Messages.get("commands.economy.give.success", amount, currency, target.getName()));
            } else {
                sender.sendMessage(Messages.get("commands.economy.give.failed", Messages.get("errors.insufficient-funds", amount, currency)));
            }
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(Messages.get("errors.invalid-amount", args[1]));
            return true;
        }
    }

    private boolean handleTopCommand(CommandSender sender, String[] args, DataBase db) {
        if (args.length < 2) {
            sender.sendMessage(Messages.get("commands.economy.usage.top"));
            return true;
        }

        String currency = args[1];
        if (!db.currencyExists(currency)) {
            sender.sendMessage(Messages.get("currency-not-found", currency));
            return true;
        }

        sender.sendMessage(Messages.get("commands.economy.top.loading"));
        Map<UUID, Double> topBalances = db.getTopBalances(currency, 10);
        
        if (topBalances.isEmpty()) {
            sender.sendMessage(Messages.get("commands.economy.top.empty"));
            return true;
        }

        sender.sendMessage(Messages.get("commands.economy.top.header", 10, currency));
        int position = 1;
        for (Map.Entry<UUID, Double> entry : topBalances.entrySet()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String playerName = player.getName() != null ? player.getName() : entry.getKey().toString();
            sender.sendMessage(Messages.get("commands.economy.top.entry", position++, playerName, entry.getValue(), currency));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Messages.get("commands.economy.help-header"));
        if (sender.hasPermission("economyengine.create"))
            sender.sendMessage(Messages.get("commands.economy.help.create"));
        if (sender.hasPermission("economyengine.set"))
            sender.sendMessage(Messages.get("commands.economy.help.set"));
        if (sender.hasPermission("economyengine.add"))
            sender.sendMessage(Messages.get("commands.economy.help.add"));
        if (sender.hasPermission("economyengine.remove"))
            sender.sendMessage(Messages.get("commands.economy.help.remove"));
        if (sender.hasPermission("economyengine.give") && sender instanceof Player)
            sender.sendMessage(Messages.get("commands.economy.help.give"));
        if (sender.hasPermission("economyengine.top"))
            sender.sendMessage(Messages.get("commands.economy.help.top"));
    }
}