package me.wryuin.utils;

import me.wryuin.EconomyEngine;
import me.wryuin.database.DatabaseType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupManager {
    private final EconomyEngine plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public BackupManager(EconomyEngine plugin) {
        this.plugin = plugin;
        startBackupTask();
    }

    private void startBackupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                DatabaseType type = plugin.getConfigManager().getDatabaseType();
                File source = type == DatabaseType.SQLITE
                        ? new File(plugin.getDataFolder(), "economy.db")
                        : new File(plugin.getDataFolder(), "data.yml");

                File backupsDir = new File(plugin.getDataFolder(), "backups");
                if(!backupsDir.exists()) backupsDir.mkdirs();

                String timestamp = dateFormat.format(new Date());
                String ext = type == DatabaseType.SQLITE ? ".db.backup" : ".yml.backup";
                File dest = new File(backupsDir, source.getName() + "_" + timestamp + ext);

                Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create backup: " + e.getMessage());
            }
        }, 20L * 60 * 3, 20L * 60 * 3);
    }
}