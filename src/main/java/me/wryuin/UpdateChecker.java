package me.wryuin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private final EconomyEngine plugin;
    private final String currentVersion;
    private final String USER_AGENT = "EconomyEngineUpdateChecker";

    public UpdateChecker(EconomyEngine plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        checkUpdates();
    }

    private void checkUpdates() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String latest = getLatestVersion();
                if(isNewer(latest)) {
                    plugin.getLogger().warning("Доступно обновление " + latest);
                    plugin.getLogger().warning("Скачайте: https://github.com/wryuin/EconomyEngine/releases/");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось проверить обновления: " + e.getMessage());
            }
        });
    }

    private String getLatestVersion() throws IOException {
        URL url = new URL("https://api.github.com/repos/wryuin/EconomyEngine/releases/latest");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
            return response.get("tag_name").getAsString().replace("v", "");
        }
    }

    private boolean isNewer(String latest) {
        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latest.split("\\.");

        for(int i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
            int current = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestVer = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

            if(latestVer > current) return true;
            if(latestVer < current) return false;
        }
        return false;
    }
}