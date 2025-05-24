package me.wryuin.events;

import me.wryuin.EconomyEngine;
import me.wryuin.database.DataBase;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private final EconomyEngine plugin;

    public JoinListener(EconomyEngine plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        DataBase db = plugin.getDatabase();
        for (String currency : db.getCurrencies().keySet()) {
            double balance = db.getBalance(event.getPlayer(), currency);
        }
    }
}