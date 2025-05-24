package me.wryuin.commands;

import me.wryuin.EconomyEngine;
import me.wryuin.database.DataBase;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EconomyCommand implements CommandExecutor {
    private final EconomyEngine plugin;

    public EconomyCommand(EconomyEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        DataBase db = plugin.getDatabase();

        switch (args[0].toLowerCase()) {
            case "create":
                if (!sender.hasPermission("economyengine.create")) {
                    sender.sendMessage("§cУ вас нет прав на эту команду!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: /economy create <название> [символ]");
                    return true;
                }
                String currencyName = args[1];
                String symbol = args.length > 2 ? args[2] : "$";
                if (db.createCurrency(currencyName, symbol)) {
                    sender.sendMessage("§aВалюта " + currencyName + " успешно создана!");
                } else {
                    sender.sendMessage("§cВалюта " + currencyName + " уже существует!");
                }
                return true;

            case "set":
                if (!sender.hasPermission("economyengine.set")) {
                    sender.sendMessage("§cУ вас нет прав на эту команду!");
                    return true;
                }
                return handleSetCommand(sender, args, db);

            case "add":
                if (!sender.hasPermission("economyengine.add")) {
                    sender.sendMessage("§cУ вас нет прав на эту команду!");
                    return true;
                }
                return handleAddRemoveCommand(sender, args, db, true);

            case "remove":
                if (!sender.hasPermission("economyengine.remove")) {
                    sender.sendMessage("§cУ вас нет прав на эту команду!");
                    return true;
                }
                return handleAddRemoveCommand(sender, args, db, false);

            case "give":
                if (!sender.hasPermission("economyengine.give") || !(sender instanceof Player)) {
                    sender.sendMessage("§cУ вас нет прав на эту команду или вы не игрок!");
                    return true;
                }
                return handleGiveCommand((Player) sender, args, db);

            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleSetCommand(CommandSender sender, String[] args, DataBase db) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользование: /economy set <количество> <валюта> <игрок>");
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            String currency = args[2];
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);

            if (!db.currencyExists(currency)) {
                sender.sendMessage("§cВалюта " + currency + " не существует!");
                return true;
            }

            db.setBalance(target, currency, amount);
            sender.sendMessage("§aБаланс игрока " + target.getName() + " установлен на " + amount + " " + currency);
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНеверное количество!");
            return true;
        }
    }

    private boolean handleAddRemoveCommand(CommandSender sender, String[] args, DataBase db, boolean isAdd) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользование: /economy " + (isAdd ? "add" : "remove") + " <количество> <валюта> <игрок>");
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            String currency = args[2];
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);

            if (!db.currencyExists(currency)) {
                sender.sendMessage("§cВалюта " + currency + " не существует!");
                return true;
            }

            if (isAdd) {
                db.addBalance(target, currency, amount);
                sender.sendMessage("§aДобавлено " + amount + " " + currency + " игроку " + target.getName());
            } else {
                db.removeBalance(target, currency, amount);
                sender.sendMessage("§aУдалено " + amount + " " + currency + " у игрока " + target.getName());
            }
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНеверное количество!");
            return true;
        }
    }

    private boolean handleGiveCommand(Player sender, String[] args, DataBase db) {
        if (args.length < 4) {
            sender.sendMessage("§cИспользование: /economy give <количество> <валюта> <игрок>");
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            String currency = args[2];
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);

            if (!db.currencyExists(currency)) {
                sender.sendMessage("§cВалюта " + currency + " не существует!");
                return true;
            }

            if (db.transferBalance(sender, target, currency, amount)) {
                sender.sendMessage("§aВы передали " + amount + " " + currency + " игроку " + target.getName());
            } else {
                sender.sendMessage("§cУ вас недостаточно средств или произошла ошибка!");
            }
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage("§cНеверное количество!");
            return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== EconomyEngine Помощь =====");
        if (sender.hasPermission("economyengine.create"))
            sender.sendMessage("§e/economy create <название> [символ] - Создать валюту");
        if (sender.hasPermission("economyengine.set"))
            sender.sendMessage("§e/economy set <количество> <валюта> <игрок> - Установить баланс");
        if (sender.hasPermission("economyengine.add"))
            sender.sendMessage("§e/economy add <количество> <валюта> <игрок> - Добавить баланс");
        if (sender.hasPermission("economyengine.remove"))
            sender.sendMessage("§e/economy remove <количество> <валюта> <игрок> - Удалить баланс");
        if (sender.hasPermission("economyengine.give") && sender instanceof Player)
            sender.sendMessage("§e/economy give <количество> <валюта> <игрок> - Передать валюту");
    }
}