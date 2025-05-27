package me.wryuin.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Objects;

public class GUISession {
    private final Player player;
    private Inventory currentInventory;
    private GUIType currentType;
    private String selectedCurrency;
    private String selectedPlayer;

    public GUISession(Player player) {
        this.player = Objects.requireNonNull(player, "Player cannot be null");
        this.currentType = GUIType.MAIN_MENU; // Default type
    }

    public Player getPlayer() {
        return player;
    }

    public Inventory getCurrentInventory() {
        return currentInventory;
    }

    public void setCurrentInventory(Inventory inventory) {
        this.currentInventory = inventory;
    }

    public GUIType getCurrentType() {
        return currentType != null ? currentType : GUIType.MAIN_MENU;
    }

    public void setCurrentType(GUIType currentType) {
        this.currentType = currentType != null ? currentType : GUIType.MAIN_MENU;
    }

    public String getSelectedCurrency() {   
        return selectedCurrency;
    }

    public void setSelectedCurrency(String selectedCurrency) {
        this.selectedCurrency = selectedCurrency;
    }

    public String getSelectedPlayer() {
        return selectedPlayer;
    }

    public void setSelectedPlayer(String selectedPlayer) {
        this.selectedPlayer = selectedPlayer;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GUISession that = (GUISession) o;
        return Objects.equals(player, that.player);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player);
    }
}