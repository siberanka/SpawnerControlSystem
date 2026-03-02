package com.siberanka.spawnercontrolsystem.managers;

import com.siberanka.spawnercontrolsystem.SpawnerControlSystem;
import com.siberanka.spawnercontrolsystem.utils.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class LoggerManager {

    private final SpawnerControlSystem plugin;
    private final SimpleDateFormat dateFormat;

    public LoggerManager(SpawnerControlSystem plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public void logEvent(Player player, BlockPos pos, boolean isPlace) {
        // Run completely asynchronously to protect MSPT
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            String actionEN = isPlace ? "placed" : "broke";
            String actionLocal = isPlace
                    ? (plugin.getLangManager().getRawMessage("webhook.place-title").contains("yerleştirildi")
                            ? "yerleştirdi"
                            : "placed")
                    : (plugin.getLangManager().getRawMessage("webhook.break-title").contains("kırıldı") ? "kırdı"
                            : "broke");

            // TXT Logging
            if (plugin.getConfig().getBoolean("logging.local-file.enabled", true)) {
                String logFormat = plugin.getLangManager().getRawMessage("log-txt");
                if (logFormat != null) {
                    String time = dateFormat.format(new Date());
                    String msg = logFormat
                            .replace("{time}", time)
                            .replace("{player}", player.getName())
                            .replace("{action}",
                                    plugin.getConfigurationManager().getLang().equals("tr") ? actionLocal : actionEN)
                            .replace("{world}", pos.world)
                            .replace("{x}", String.valueOf(pos.x))
                            .replace("{y}", String.valueOf(pos.y))
                            .replace("{z}", String.valueOf(pos.z));
                    writeToLogFile(msg);
                }
            }

            // Webhook Logging
            if (plugin.getConfig().getBoolean("logging.discord-webhook.enabled", false)) {
                String urlStr = plugin.getConfig().getString("logging.discord-webhook.url", "");
                if (urlStr != null && !urlStr.isEmpty() && urlStr.startsWith("http")) {
                    sendWebhook(player, pos, isPlace, urlStr);
                }
            }
        });
    }

    private void writeToLogFile(String message) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File logFile = new File(dataFolder, "spawners.log");
        try (FileWriter fw = new FileWriter(logFile, true); PrintWriter pw = new PrintWriter(fw)) {
            pw.println(message);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write to spawners.log", e);
        }
    }

    private void sendWebhook(Player player, BlockPos pos, boolean isPlace, String urlStr) {
        try {
            int color = isPlace ? plugin.getConfig().getInt("logging.discord-webhook.color-place", 65280)
                    : plugin.getConfig().getInt("logging.discord-webhook.color-break", 16711680);

            String titleKey = isPlace ? "webhook.place-title" : "webhook.break-title";
            String actionWord = isPlace
                    ? (plugin.getConfigurationManager().getLang().equals("tr") ? "yerleştirdi" : "placed")
                    : (plugin.getConfigurationManager().getLang().equals("tr") ? "kırdı" : "broke");

            String title = escapeJson(plugin.getLangManager().getRawMessage(titleKey));
            String descTemplate = plugin.getLangManager().getRawMessage("webhook.description");

            String description = descTemplate
                    .replace("{player}", player.getName())
                    .replace("{world}", pos.world)
                    .replace("{x}", String.valueOf(pos.x))
                    .replace("{y}", String.valueOf(pos.y))
                    .replace("{z}", String.valueOf(pos.z))
                    .replace("{action}", actionWord);

            description = escapeJson(description);
            String name = escapeJson(plugin.getLangManager().getRawMessage("webhook.name"));

            // Minimal JSON Payload Construction for Discord Webhook without resorting to
            // heavy external JSON libraries
            String jsonPayload = "{"
                    + "\"username\": \"" + name + "\","
                    + "\"embeds\": [{"
                    + "\"title\": \"" + title + "\","
                    + "\"description\": \"" + description + "\","
                    + "\"color\": " + color + ","
                    + "\"timestamp\": \""
                    + java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()) + "\""
                    + "}]"
                    + "}";

            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "SpawnerControlSystem");
            connection.setDoOutput(true);

            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                plugin.getLogger().log(Level.WARNING, "Discord webhook failed with HTTP code " + responseCode);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to send Discord webhook", e);
        }
    }

    private String escapeJson(String input) {
        if (input == null)
            return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
