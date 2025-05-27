package me.wryuin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UpdateChecker {
    private final EconomyEngine plugin;
    private final String currentVersion;
    private final String USER_AGENT = "EconomyEngineUpdateChecker";
    private final Gson gson = new Gson();

    public UpdateChecker(EconomyEngine plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        checkUpdates();
    }

    private void checkUpdates() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String latest = getLatestVersion();
                if (latest != null && isNewer(latest)) {
                    plugin.getLogger().warning("Доступно обновление " + latest);
                    plugin.getLogger().warning("Скачайте: https://github.com/wryuin/EconomyEngine/releases/");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Не удалось проверить обновления", e);
            }
        });
    }

    private String getLatestVersion() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.github.com/repos/wryuin/EconomyEngine/releases/latest");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                plugin.getLogger().warning("Не удалось получить данные об обновлениях. Код ответа: " + conn.getResponseCode());
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                JsonObject response = gson.fromJson(reader, JsonObject.class);
                return response.get("tag_name").getAsString().replace("v", "");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка при проверке обновлений", e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private boolean isNewer(String latest) {
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] latestParts = latest.split("\\.");

            for (int i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
                int current = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestVer = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

                if (latestVer > current) return true;
                if (latestVer < current) return false;
            }
            return false;
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Ошибка при сравнении версий: " + e.getMessage());
            return false;
        }
    }
}