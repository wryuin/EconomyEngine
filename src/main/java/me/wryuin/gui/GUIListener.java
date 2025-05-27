package me.wryuin.gui;

import me.wryuin.EconomyEngine;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

public class GUIListener implements Listener {
    private final EconomyEngine plugin;
    private final EconomyGUI gui;

    public GUIListener(EconomyEngine plugin, EconomyGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        GUISession session = gui.getSession(player);

        if (session == null || session.getCurrentInventory() == null || event.getClickedInventory() == null) {
            return;
        }

        if (!event.getInventory().equals(session.getCurrentInventory())) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        try {
            switch (session.getCurrentType()) {
                case MAIN_MENU:
                    handleMainMenuClick(player, event.getSlot(), clickedItem);
                    break;
                case CURRENCY_MENU:
                    handleCurrencyMenuClick(player, event.getSlot(), clickedItem);
                    break;
                case PLAYER_MENU:
                    handlePlayerMenuClick(player, event.getSlot(), clickedItem);
                    break;
                case PLAYER_BALANCE_MENU:
                    handlePlayerBalanceMenuClick(player, event.getSlot(), clickedItem);
                    break;
                case TOP_MENU:
                    handleTopMenuClick(player, event.getSlot(), clickedItem);
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling GUI click", e);
            player.sendMessage("§cПроизошла ошибка при обработке клика. Пожалуйста, попробуйте снова.");
        }
    }

    private void handleMainMenuClick(Player player, int slot, ItemStack clickedItem) {
        // Check for specific slots from config
        switch (slot) {
            case 10: // Currencies
                gui.openCurrencyMenu(player);
                break;
            case 12: // Top Balances
                // Open currency selection for top balances
                gui.openCurrencyMenu(player);
                break;
            case 14: // Player Management
                gui.openPlayerMenu(player);
                break;
            case 16: // Settings
                // Could open a settings menu in the future
                break;
            case 22: // Close
                player.closeInventory();
                break;
        }
    }

    private void handleCurrencyMenuClick(Player player, int slot, ItemStack clickedItem) {
        GUISession session = gui.getSession(player);
        if (session == null) return;
        
        if (slot == 27) { // Back button
            gui.openMainMenu(player);
            return;
        }
        
        if (slot == 31) { // Create currency
            player.closeInventory();
            player.sendMessage("§aUse /economy createcurrency <n> <symbol> to create a new currency.");
            return;
        }
        
        if (slot == 35) { // Close button
            player.closeInventory();
            return;
        }
        
        // Check if it's a currency item
        if (clickedItem.getType() == Material.GOLD_INGOT && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
            String displayName = clickedItem.getItemMeta().getDisplayName();
            if (displayName.length() > 2) {
                String currencyName = displayName.substring(2); // Remove color code
                session.setSelectedCurrency(currencyName);
                gui.openTopBalancesMenu(player, currencyName);
            }
        }
    }

    private void handlePlayerMenuClick(Player player, int slot, ItemStack clickedItem) {
        if (slot == 45) { // Back button
            gui.openMainMenu(player);
            return;
        }
        
        if (slot == 53) { // Close button
            player.closeInventory();
            return;
        }
        
        // Check if it's a player head
        if (clickedItem.getType() == Material.PLAYER_HEAD && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
            String displayName = clickedItem.getItemMeta().getDisplayName();
            if (displayName.length() > 2) {
                String playerName = displayName.substring(2); // Remove color code
                gui.openPlayerBalanceMenu(player, playerName);
            }
        }
    }

    private void handlePlayerBalanceMenuClick(Player player, int slot, ItemStack clickedItem) {
        GUISession session = gui.getSession(player);
        if (session == null) return;
        
        if (slot == 45) { // Back button
            gui.openPlayerMenu(player);
            return;
        }
        
        if (slot == 53) { // Close button
            player.closeInventory();
            return;
        }
        
        if (slot == 48) { // Add money
            if (session.getSelectedPlayer() != null) {
                player.closeInventory();
                String currency = session.getSelectedCurrency() != null ? session.getSelectedCurrency() : "default";
                player.sendMessage("§aUse /economy give " + session.getSelectedPlayer() + " <amount> " + currency);
            }
            return;
        }
        
        if (slot == 50) { // Remove money
            if (session.getSelectedPlayer() != null) {
                player.closeInventory();
                String currency = session.getSelectedCurrency() != null ? session.getSelectedCurrency() : "default";
                player.sendMessage("§aUse /economy take " + session.getSelectedPlayer() + " <amount> " + currency);
            }
            return;
        }
        
        // Check if it's a currency item
        if (clickedItem.getType() == Material.PAPER && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
            String displayName = clickedItem.getItemMeta().getDisplayName();
            if (displayName.length() > 2) {
                String currencyName = displayName.substring(2); // Remove color code
                session.setSelectedCurrency(currencyName);
            }
        }
    }

    private void handleTopMenuClick(Player player, int slot, ItemStack clickedItem) {
        if (slot == 45) { // Back button
            gui.openCurrencyMenu(player);
            return;
        }
        
        if (slot == 53) { // Close button
            player.closeInventory();
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        GUISession session = gui.getSession(player);
        
        if (session != null && session.getCurrentInventory() != null && event.getInventory().equals(session.getCurrentInventory())) {
            // Keep the session for now, just clear the inventory reference
            session.setCurrentInventory(null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gui.removeSession(event.getPlayer());
    }
}