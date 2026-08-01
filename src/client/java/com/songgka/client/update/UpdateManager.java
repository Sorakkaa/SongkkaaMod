package com.songgka.client.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.songgka.client.SonggkaClient;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

public class UpdateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Songgka-Updater");

    private static final String UPDATE_URL = "https://jsonblob.com/api/jsonBlob/019fbcd2-f85d-7239-a3e7-679dd04b6c37"; 
    
    public static volatile boolean updateReady = false;
    public static String downloadedVersion = null;

    public static void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Checking for Songgka updates...");
                URL url = URI.create(UPDATE_URL).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                    }

                    JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
                    if (json.has("latest_version") && json.has("download_url")) {
                        String latestVersion = json.get("latest_version").getAsString();
                        String downloadUrl = json.get("download_url").getAsString();

                        if (isNewerVersion(SonggkaClient.MOD_VERSION, latestVersion)) {
                            LOGGER.info("New version found: " + latestVersion + ". Downloading...");
                            downloadUpdate(downloadUrl, latestVersion);
                        } else {
                            LOGGER.info("Songgka is up to date (v" + SonggkaClient.MOD_VERSION + ")");
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to check for updates", e);
            }
        });
    }

    private static boolean isNewerVersion(String current, String latest) {
        // Simple string comparison for versions like "1.0.0" vs "1.0.1"
        // In a real scenario, you might want to split by "." and compare integers
        try {
            String[] currParts = current.split("\\.");
            String[] lateParts = latest.split("\\.");
            
            int length = Math.max(currParts.length, lateParts.length);
            for (int i = 0; i < length; i++) {
                int c = i < currParts.length ? Integer.parseInt(currParts[i]) : 0;
                int l = i < lateParts.length ? Integer.parseInt(lateParts[i]) : 0;
                if (l > c) return true;
                if (l < c) return false;
            }
            return false;
        } catch (Exception e) {
            return !current.equals(latest); // Fallback
        }
    }

    private static void downloadUpdate(String fileUrl, String newVersion) {
        try {
            URL url = URI.create(fileUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            
            // Handle redirects if downloading from GitHub etc
            conn.setInstanceFollowRedirects(true);

            if (conn.getResponseCode() == 200 || conn.getResponseCode() == 302) {
                Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
                Path downloadDest = modsDir.resolve("songgka-" + newVersion + ".jar");

                try (InputStream in = conn.getInputStream()) {
                    Files.copy(in, downloadDest, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Successfully downloaded update to " + downloadDest.toString());
                    downloadedVersion = newVersion;
                    updateReady = true;
                }
            } else {
                LOGGER.error("Failed to download update, HTTP response code: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            LOGGER.error("Error downloading update", e);
        }
    }
}
