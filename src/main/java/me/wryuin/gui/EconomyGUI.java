package me.wryuin.gui;

import me.wryuin.Currency;
import me.wryuin.EconomyEngine;
import me.wryuin.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class EconomyGUI {
    private final EconomyEngine plugin;
    private final Map<UUID, GUISession> sessions = new ConcurrentHashMap<>();
    private FileConfiguration guiConfig;
    private final Map<String, ItemStack> cachedConfigItems = new HashMap<>();

    public EconomyGUI(EconomyEngine plugin) {
        this.plugin = plugin;
        loadGuiConfig();
    }

    private void loadGuiConfig() {
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
            plugin.getLogger().info("Created new gui.yml file");
        }
        
        try {
            this.guiConfig = YamlConfiguration.loadConfiguration(guiFile);
            cachedConfigItems.clear();
            plugin.getLogger().info("GUI configuration loaded successfully");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load gui.yml: " + e.getMessage());
            // Attempt recovery by using a new empty configuration
            this.guiConfig = new YamlConfiguration();
        }
    }

    public void reloadConfig() {
        loadGuiConfig();
        // Clear all sessions to force reconstruction of inventories with updated config
        for (GUISession session : sessions.values()) {
            if (session.getPlayer() != null && session.getPlayer().isOnline()) {
                session.getPlayer().closeInventory();
            }
        }
        sessions.clear();
    }

    public GUISession getSession(Player player) {
        if (player == null) {
            return null;
        }
        return sessions.computeIfAbsent(player.getUniqueId(), k -> new GUISession(player));
    }

    public void removeSession(Player player) {
        if (player == null) {
            return;
        }
        sessions.remove(player.getUniqueId());
    }

    public void openMainMenu(Player player) {
        if (player == null) {
            return;
        }
        
        GUISession session = getSession(player);
        ConfigurationSection menuConfig = guiConfig.getConfigurationSection("gui.main_menu");
        if (menuConfig == null) {
            plugin.getLogger().warning("Missing gui.main_menu section in gui.yml");
            return;
        }

        String title = ChatColor.translateAlternateColorCodes('&', menuConfig.getString("title", "Economy Management"));
        int size = menuConfig.getInt("size", 27);

        Inventory inventory = Bukkit.createInventory(null, size, title);

        // Add items from config
        ConfigurationSection itemsSection = menuConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = createItemFromConfig(itemSection);
                    int slot = itemSection.getInt("slot", 0);
                    if (slot >= 0 && slot < size) {
                        inventory.setItem(slot, item);
                    }
                }
            }
        }

        session.setCurrentInventory(inventory);
        session.setCurrentType(GUIType.MAIN_MENU);
        player.openInventory(inventory);
    }

    public void openCurrencyMenu(Player player) {
        if (player == null) {
            return;
        }
        
        GUISession session = getSession(player);
        ConfigurationSection menuConfig = guiConfig.getConfigurationSection("gui.currency_menu");
        if (menuConfig == null) {
            plugin.getLogger().warning("Missing gui.currency_menu section in gui.yml");
            return;
        }

        String title = ChatColor.translateAlternateColorCodes('&', menuConfig.getString("title", "Currency Management"));
        int size = menuConfig.getInt("size", 36);

        Inventory inventory = Bukkit.createInventory(null, size, title);

        // Add items from config
        ConfigurationSection itemsSection = menuConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = createItemFromConfig(itemSection);
                    int slot = itemSection.getInt("slot", 0);
                    if (slot >= 0 && slot < size) {
                        inventory.setItem(slot, item);
                    }
                }
            }
        }

        // Add currencies
        Map<String, Currency> currencies = plugin.getDatabase().getCurrencies();
        int startSlot = 10;
        int i = 0;
        
        if (currencies != null) {
            for (Currency currency : currencies.values()) {
                if (currency == null || i >= 16) continue;
                
                ItemStack item = new ItemStack(Material.GOLD_INGOT);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GOLD + currency.getName());
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Symbol: " + currency.getSymbol());
                    lore.add(ChatColor.YELLOW + "Click to manage this currency");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }

                int slot = startSlot + (i % 7) + (i / 7) * 9;
                if (slot >= 0 && slot < size) {
                    inventory.setItem(slot, item);
                }
                i++;
            }
        }

        session.setCurrentInventory(inventory);
        session.setCurrentType(GUIType.CURRENCY_MENU);
        player.openInventory(inventory);
    }

    public void openPlayerMenu(Player player) {
        if (player == null) {
            return;
        }
        
        GUISession session = getSession(player);
        ConfigurationSection menuConfig = guiConfig.getConfigurationSection("gui.player_menu");
        if (menuConfig == null) {
            plugin.getLogger().warning("Missing gui.player_menu section in gui.yml");
            return;
        }

        String title = ChatColor.translateAlternateColorCodes('&', menuConfig.getString("title", "Player Management"));
        int size = menuConfig.getInt("size", 54);

        Inventory inventory = Bukkit.createInventory(null, size, title);

        // Add items from config
        ConfigurationSection itemsSection = menuConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = createItemFromConfig(itemSection);
                    int slot = itemSection.getInt("slot", 0);
                    if (slot >= 0 && slot < size) {
                        inventory.setItem(slot, item);
                    }
                }
            }
        }

        // Add online players
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int startSlot = 10;
        for (int i = 0; i < onlinePlayers.size() && i < 36; i++) {
            Player p = onlinePlayers.get(i);
            if (p == null) continue;
            
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(p);
                meta.setDisplayName(ChatColor.GREEN + p.getName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Click to manage this player's balances");
                meta.setLore(lore);
                skull.setItemMeta(meta);
            }

            int slot = startSlot + (i % 7) + (i / 7) * 9;
            if (slot >= 0 && slot < size) {
                inventory.setItem(slot, skull);
            }
        }

        session.setCurrentInventory(inventory);
        session.setCurrentType(GUIType.PLAYER_MENU);
        player.openInventory(inventory);
    }

    public void openPlayerBalanceMenu(Player player, String targetPlayer) {
        if (player == null || targetPlayer == null) {
            return;
        }
        
        GUISession session = getSession(player);
        ConfigurationSection menuConfig = guiConfig.getConfigurationSection("gui.player_balance_menu");
        if (menuConfig == null) {
            plugin.getLogger().warning("Missing gui.player_balance_menu section in gui.yml");
            return;
        }

        String title = ChatColor.translateAlternateColorCodes('&',
                menuConfig.getString("title", "%player%'s Balances").replace("%player%", targetPlayer));
        int size = menuConfig.getInt("size", 54);

        Inventory inventory = Bukkit.createInventory(null, size, title);

        // Add items from config
        ConfigurationSection itemsSection = menuConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = createItemFromConfig(itemSection);
                    int slot = itemSection.getInt("slot", 0);
                    if (slot >= 0 && slot < size) {
                        inventory.setItem(slot, item);
                    }
                }
            }
        }

        // Add currencies with balances
        Map<String, Currency> currencies = plugin.getDatabase().getCurrencies();
        int startSlot = 10;
        int i = 0;
        
        if (currencies != null) {
            for (Currency currency : currencies.values()) {
                if (currency == null || i >= 28) continue;
                
                OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(targetPlayer);
                double balance = plugin.getDatabase().getBalance(targetOfflinePlayer, currency.getName());

                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GOLD + currency.getName());
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Balance: " + currency.getSymbol() + balance);
                    lore.add(ChatColor.YELLOW + "Click to modify balance");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }

                int slot = startSlot + (i % 7) + (i / 7) * 9;
                if (slot >= 0 && slot < size) {
                    inventory.setItem(slot, item);
                }
                i++;
            }
        }

        session.setCurrentInventory(inventory);
        session.setCurrentType(GUIType.PLAYER_BALANCE_MENU);
        session.setSelectedPlayer(targetPlayer);
        player.openInventory(inventory);
    }

    public void openTopBalancesMenu(Player player, String currencyName) {
        if (player == null || currencyName == null) {
            return;
        }
        
        GUISession session = getSession(player);
        ConfigurationSection menuConfig = guiConfig.getConfigurationSection("gui.top_menu");
        if (menuConfig == null) {
            plugin.getLogger().warning("Missing gui.top_menu section in gui.yml");
            return;
        }

        String title = ChatColor.translateAlternateColorCodes('&',
                menuConfig.getString("title", "Top Balances - %currency%").replace("%currency%", currencyName));
        int size = menuConfig.getInt("size", 54);

        Inventory inventory = Bukkit.createInventory(null, size, title);

        // Add items from config
        ConfigurationSection itemsSection = menuConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = createItemFromConfig(itemSection);
                    int slot = itemSection.getInt("slot", 0);
                    if (slot >= 0 && slot < size) {
                        inventory.setItem(slot, item);
                    }
                }
            }
        }

        // Add top players
        Map<UUID, Double> topBalances = plugin.getDatabase().getTopBalances(currencyName, 36);
        int i = 0;
        int startSlot = 10;
        
        for (Map.Entry<UUID, Double> entry : topBalances.entrySet()) {
            if (i >= 36) break;
            
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
                meta.setOwningPlayer(offlinePlayer);
                meta.setDisplayName(ChatColor.GOLD + (offlinePlayer.getName() != null ? offlinePlayer.getName() : entry.getKey().toString()));
                
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.YELLOW + "Balance: " + entry.getValue() + " " + currencyName);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            int slot = startSlot + (i % 7) + (i / 7) * 9;
            if (slot >= 0 && slot < size) {
                inventory.setItem(slot, item);
            }
            i++;
        }

        session.setCurrentInventory(inventory);
        session.setCurrentType(GUIType.TOP_BALANCES_MENU);
        session.setSelectedCurrency(currencyName);
        player.openInventory(inventory);
    }

    private ItemStack createItemFromConfig(ConfigurationSection section) {
        if (section == null) {
            return new ItemStack(Material.STONE);
        }
        
        String cacheKey = section.getCurrentPath();
        if (cachedConfigItems.containsKey(cacheKey)) {
            return cachedConfigItems.get(cacheKey).clone();
        }
        
        String materialName = section.getString("material", "STONE");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (section.contains("name")) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("name")));
        }

        if (section.contains("lore")) {
            List<String> lore = section.getStringList("lore").stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        
        // Cache the item for future use
        cachedConfigItems.put(cacheKey, item.clone());
        
        return item;
    }
}