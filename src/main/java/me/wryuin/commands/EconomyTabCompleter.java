package me.wryuin.commands;

import me.wryuin.EconomyEngine;
import  me.wryuin.database.DataBase;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EconomyTabCompleter implements TabCompleter {

    private final EconomyEngine plugin;
    private final List<String> subCommands = List.of("create", "set", "add", "remove", "give", "top");

    public EconomyTabCompleter(EconomyEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            return filterByPermission(sender, subCommands, args[0]);
        }

        DataBase db = plugin.getDatabase();

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length == 2) {
                    completions.add("<currency_name>");
                } else if (args.length == 3) {
                    completions.add("[symbol]");
                }
                break;

            case "set":
            case "add":
            case "remove":
                completions.addAll(handleAmountCurrencyPlayer(sender, args, db));
                break;

            case "give":
                completions.addAll(handleGiveCommand(sender, args, db));
                break;

            case "top":
                if (args.length == 2) {
                    completions.addAll(db.getCurrencies().keySet());
                }
                break;
        }

        return completions.isEmpty() ? null : completions;
    }

    private List<String> handleAmountCurrencyPlayer(CommandSender sender, String[] args, DataBase db) {
        List<String> suggestions = new ArrayList<>();

        switch (args.length) {
            case 2:
                suggestions.add("<amount>");
                break;
            case 3:
                suggestions.addAll(db.getCurrencies().keySet());
                break;
            case 4:
                suggestions.addAll(getOnlinePlayerNames());
                break;
        }

        return filterSuggestions(args[args.length-1], suggestions);
    }

    private List<String> handleGiveCommand(CommandSender sender, String[] args, DataBase db) {
        List<String> suggestions = new ArrayList<>();
        Player player = (sender instanceof Player) ? (Player) sender : null;

        if (player == null) return Collections.emptyList();

        switch (args.length) {
            case 2:
                suggestions.add("<amount>");
                break;
            case 3:
                suggestions.addAll(getAvailableCurrencies(player, db));
                break;
            case 4:
                suggestions.addAll(getOnlinePlayerNamesExcept(player));
                break;
        }

        return filterSuggestions(args[args.length-1], suggestions);
    }

    private List<String> filterByPermission(CommandSender sender, List<String> commands, String input) {
        return commands.stream()
                .filter(cmd -> hasPermission(sender, "economyengine." + cmd))
                .filter(cmd -> StringUtil.startsWithIgnoreCase(cmd, input))
                .collect(Collectors.toList());
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.isOp();
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> getOnlinePlayerNamesExcept(Player except) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(except))
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> getAvailableCurrencies(Player player, DataBase db) {
        return db.getCurrencies().keySet().stream()
                .filter(currency -> db.getBalance(player, currency) > 0)
                .collect(Collectors.toList());
    }

    private List<String> filterSuggestions(String input, List<String> suggestions) {
        return suggestions.stream()
                .filter(s -> StringUtil.startsWithIgnoreCase(s, input))
                .collect(Collectors.toList());
    }
}