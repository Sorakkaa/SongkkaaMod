package com.songgka.client;

import com.songgka.client.config.ModConfig;
import com.songgka.client.gui.SonggkaConfigScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class SonggkaClient implements ClientModInitializer {

	public static final String MOD_VERSION = "1.1.0";
	private static final String APP_ID = "songkkaa";

	// Anti-spam state
	private static long lastRequestTime = 0;
	private static volatile boolean isPairingActive = false;
	public static String currentSongText = null;

	@Override
	public void onInitializeClient() {
		ModConfig.load();
		com.songgka.client.color.NameColorManager.init();
		com.songgka.client.update.UpdateManager.checkForUpdates();

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			com.songgka.client.color.NameColorManager.syncLocalPlayerColor();
		});

		ClientSendMessageEvents.CHAT.register((message) -> {
			if (!ModConfig.INSTANCE.enableSong) return;
			if (!ModConfig.INSTANCE.enableAc) return;
			if (message != null && message.trim().toLowerCase().contains("!song")) {
				fetchAndSendSongWithDelay(null);
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommands.literal("songkkaa")
				.executes(context -> {
					Minecraft mc = Minecraft.getInstance();
					mc.execute(() -> mc.setScreen(new SonggkaConfigScreen(mc.screen)));
					return 1;
				})
			);
		});

		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
			if (overlay) return message;
			return com.songgka.client.color.NameColorManager.colorizeText(message);
		});

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!ModConfig.INSTANCE.enableSong) return;
			String text = message.getString();
			if (text == null || text.contains("[Songkkaa]")) return;

			// Normalize non-breaking spaces and lower-case text
			String lower = text.replace('\u00A0', ' ').toLowerCase().trim();
			if (lower.contains("!song")) {
				boolean isParty = lower.contains("party") || lower.contains("p >") || lower.contains("[party]");
				boolean isGuild = lower.contains("guild") || lower.contains("officer") || lower.contains("g >") || lower.contains("o >") || lower.contains("[guild]");

				if (isParty) {
					if (ModConfig.INSTANCE.enablePc) {
						fetchAndSendSongWithDelay("/pc ");
					}
				} else if (isGuild) {
					if (ModConfig.INSTANCE.enableGc) {
						fetchAndSendSongWithDelay("/gc ");
					}
				} else {
					// Public / All Chat
					if (ModConfig.INSTANCE.enableAc) {
						fetchAndSendSongWithDelay(null);
					}
				}
			}
		});
	}

	public static void handleOutgoingChatMessage(String message) {
	}

	public static void handleOutgoingCommand(String command) {
	}

	private static void saveToken(String token) {
		ModConfig.INSTANCE.authToken = token;
		ModConfig.INSTANCE.save();
	}

	private static volatile boolean isFetchingState = false;

	private static void fetchAndSendSongWithDelay(String chatPrefix) {
		String provider = ModConfig.INSTANCE.musicProvider;
		if (provider == null || "None".equalsIgnoreCase(provider)) {
			sendLocalChatMessage("§d§l🎵 SONGKKAA §8» §cNo music platform selected! Please go to /songkkaa -> Song Config to select your platform.");
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastRequestTime < 2500 || isFetchingState) {
			return;
		}
		lastRequestTime = now;
		isFetchingState = true;

		CompletableFuture.runAsync(() -> {
			try {
				Thread.sleep(600);

				// 1. Try Windows System Media Session first (Works 100% natively for YTM, Deezer, Spotify, Web, etc.)
				String winMediaSong = fetchWindowsMediaSessionSong();
				if (winMediaSong != null && !winMediaSong.trim().isEmpty()) {
					sendWinMediaSongToChat(winMediaSong, chatPrefix);
					return;
				}

				// 2. Try HTTP Companion API if paired
				if (ModConfig.INSTANCE.authToken != null) {
					try {
						HttpURLConnection conn = createAuthenticatedConnection("/state", "GET");
						int responseCode = conn.getResponseCode();
						if (responseCode == 200) {
							String content = readResponse(conn);
							parseAndSendToChat(content, chatPrefix);
							return;
						}
					} catch (Exception ignored) {}
				}

				// 3. Fallback message when no song is playing anywhere
				sendLocalChatMessage("§e[Songkkaa] §cNo song currently playing on " + provider + ". Start your music and try again!");
			} catch (Exception e) {
				sendLocalChatMessage("§c[Songkkaa] Error reading music.");
			} finally {
				isFetchingState = false;
			}
		});
	}

	private static String fetchWindowsMediaSessionSong() {
		try {
			Path scriptPath = Path.of("config", "get_song.ps1");
			try (InputStream in = SonggkaClient.class.getResourceAsStream("/assets/songkkaa/scripts/get_song.ps1")) {
				if (in != null) {
					Files.createDirectories(scriptPath.getParent());
					Files.copy(in, scriptPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (Exception ignored) {}

			if (!Files.exists(scriptPath)) return null;

			String providerParam = ModConfig.INSTANCE.musicProvider != null ? ModConfig.INSTANCE.musicProvider : "YTM";
			ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", scriptPath.toAbsolutePath().toString(), "-provider", providerParam);
			Process p = pb.start();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
				String line = br.readLine();
				if (line != null && !line.trim().isEmpty()) {
					return line.trim();
				}
			} finally {
				if (p.isAlive()) {
					p.destroyForcibly();
				}
			}
		} catch (Exception ignored) {}
		return null;
	}

	private static String getBaseUrl() {
		String provider = ModConfig.INSTANCE.musicProvider;
		int port = 9863;
		if ("Deezer".equalsIgnoreCase(provider)) {
			port = ModConfig.INSTANCE.deezerPort > 0 ? ModConfig.INSTANCE.deezerPort : 9865;
		} else {
			port = ModConfig.INSTANCE.ytmPort > 0 ? ModConfig.INSTANCE.ytmPort : 9863;
		}
		return "http://127.0.0.1:" + port + "/api/v1";
	}

	public static void requestPairing() {
		String provider = ModConfig.INSTANCE.musicProvider != null ? ModConfig.INSTANCE.musicProvider : "None";
		if ("None".equalsIgnoreCase(provider)) {
			sendLocalChatMessage("§d§l🎵 SONGKKAA §8» §cPlease select a platform (YouTube Music, Spotify, or Deezer) in /songkkaa -> Song Config first!");
			return;
		}
		if ("Deezer".equalsIgnoreCase(provider) || "Spotify".equalsIgnoreCase(provider)) {
			sendLocalChatMessage("§d§l🎵 SONGKKAA §8» §a" + provider + " uses native Windows auto-detection. Play your music on " + provider + "!");
			return;
		}
		if (isPairingActive) {
			sendLocalChatMessage("§e[Songkkaa] §fPairing in progress... Allow the connection in " + provider + "!");
			return;
		}
		isPairingActive = true;

		CompletableFuture.runAsync(() -> {
			try {
				URL url = URI.create(getBaseUrl() + "/auth/requestcode").toURL();
				HttpURLConnection conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.setConnectTimeout(2000);
				conn.setReadTimeout(2000);
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setRequestProperty("User-Agent", "Songkkaa/1.0.0");

				JsonObject jsonBody = new JsonObject();
				jsonBody.addProperty("appId", APP_ID);
				jsonBody.addProperty("appName", "Songkkaa");
				jsonBody.addProperty("appVersion", "1.0.0");

				try (OutputStream os = conn.getOutputStream()) {
					os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
				}

				if (conn.getResponseCode() == 200) {
					String respStr = readResponse(conn);
					JsonObject respJson = JsonParser.parseString(respStr).getAsJsonObject();
					if (respJson.has("code")) {
						String code = respJson.get("code").getAsString();
						sendLocalChatMessage("§d§l🎵 SONGKKAA §8» §fCode: §e§l" + code + " §8(§aApprove in " + provider + " popup!§8)");
						exchangeCodeForToken(code);
						return;
					}
				} else {
					sendLocalChatMessage("§d§l🎵 SONGKKAA §8» §cFailed to request " + provider + " (code " + conn.getResponseCode() + ")");
				}
			} catch (Exception e) {
				sendLocalChatMessage("§d§l🎵 SONGKKAA §8» §cError: Make sure " + provider + " application is open!");
			}
			isPairingActive = false;
		});
	}

	private static void exchangeCodeForToken(String code) {
		CompletableFuture.runAsync(() -> {
			try {
				for (int i = 0; i < 18; i++) {
					try {
						Thread.sleep(2500);
						URL url = URI.create(getBaseUrl() + "/auth/request").toURL();
						HttpURLConnection conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
						conn.setRequestMethod("POST");
						conn.setDoOutput(true);
						conn.setConnectTimeout(2000);
						conn.setReadTimeout(2000);
						conn.setRequestProperty("Content-Type", "application/json");
						conn.setRequestProperty("User-Agent", "Songkkaa/1.0.0");

						JsonObject jsonBody = new JsonObject();
						jsonBody.addProperty("appId", APP_ID);
						jsonBody.addProperty("code", code);

						try (OutputStream os = conn.getOutputStream()) {
							os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
						}

						if (conn.getResponseCode() == 200) {
							String respStr = readResponse(conn);
							JsonObject respJson = JsonParser.parseString(respStr).getAsJsonObject();
							if (respJson.has("token")) {
								String token = respJson.get("token").getAsString();
								saveToken(token);
								sendLocalChatMessage("§d§l🎵 SONGKKAA §8» §a§lSuccessfully paired with YouTube Music!");
								return;
							}
						}
					} catch (Exception ignored) {
					}
				}
				sendLocalChatMessage("§d§l🎵 SONGKKAA §8» §cAuthorization timeout. Type !song again.");
			} finally {
				isPairingActive = false;
			}
		});
	}

	private static HttpURLConnection createAuthenticatedConnection(String endpoint, String method) throws Exception {
		URL url = URI.create(getBaseUrl() + endpoint).toURL();
		HttpURLConnection conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
		conn.setRequestMethod(method);
		conn.setConnectTimeout(1000);
		conn.setReadTimeout(1000);
		conn.setRequestProperty("User-Agent", "Songkkaa/1.0.0");
		if (ModConfig.INSTANCE.authToken != null) {
			conn.setRequestProperty("Authorization", ModConfig.INSTANCE.authToken);
		}
		return conn;
	}

	private static String readResponse(HttpURLConnection conn) throws Exception {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder content = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line);
			}
			return content.toString();
		}
	}

	private static void parseAndSendToChat(String jsonBody, String chatPrefix) {
		try {
			JsonObject json = JsonParser.parseString(jsonBody).getAsJsonObject();
			String title = "";
			String artist = "";

			if (json.has("video") && !json.get("video").isJsonNull()) {
				JsonObject video = json.getAsJsonObject("video");
				if (video.has("title") && !video.get("title").isJsonNull()) title = video.get("title").getAsString();
				if (video.has("author") && !video.get("author").isJsonNull()) artist = video.get("author").getAsString();
				if (artist.isEmpty() && video.has("artist") && !video.get("artist").isJsonNull()) artist = video.get("artist").getAsString();
			}

			if (title.isEmpty() && json.has("player") && !json.get("player").isJsonNull()) {
				JsonObject player = json.getAsJsonObject("player");
				if (player.has("track") && !player.get("track").isJsonNull()) {
					JsonObject track = player.getAsJsonObject("track");
					if (track.has("title") && !track.get("title").isJsonNull()) title = track.get("title").getAsString();
					if (track.has("author") && !track.get("author").isJsonNull()) artist = track.get("author").getAsString();
					if (artist.isEmpty() && track.has("artist") && !track.get("artist").isJsonNull()) artist = track.get("artist").getAsString();
				}
			}

			if (title.isEmpty() && json.has("title") && !json.get("title").isJsonNull()) {
				title = json.get("title").getAsString();
				if (json.has("artist") && !json.get("artist").isJsonNull()) artist = json.get("artist").getAsString();
				if (artist.isEmpty() && json.has("author") && !json.get("author").isJsonNull()) artist = json.get("author").getAsString();
			}

			if (!title.isEmpty()) {
				currentSongText = "§f🎵 §b" + title + (artist.isEmpty() ? "" : " §7- " + artist);
				String format = ModConfig.INSTANCE.messageFormat;
				if (format == null || format.trim().isEmpty()) {
					format = "{title} - {artist}";
				}
				String body = format.replace("{title}", title).replace("{artist}", artist);
				sendServerChatMessage(chatPrefix, "[Songkkaa] " + body);
				return;
			}
		} catch (Exception ignored) {}

		// Fallback to Windows Media Session
		String winMediaSong = fetchWindowsMediaSessionSong();
		if (winMediaSong != null && !winMediaSong.trim().isEmpty()) {
			sendWinMediaSongToChat(winMediaSong, chatPrefix);
			return;
		}

		sendLocalChatMessage("§e[Songkkaa] §cNo song currently playing.");
	}

	private static void sendWinMediaSongToChat(String winMediaSong, String chatPrefix) {
		currentSongText = "§f🎵 §b" + winMediaSong;
		String format = ModConfig.INSTANCE.messageFormat;
		if (format == null || format.trim().isEmpty()) {
			format = "{title} - {artist}";
		}
		String title = winMediaSong;
		String artist = "";
		if (winMediaSong.contains(" - ")) {
			String[] parts = winMediaSong.split(" - ", 2);
			title = parts[0].trim();
			artist = parts[1].trim();
		}
		String body = format.replace("{title}", title).replace("{artist}", artist);
		sendServerChatMessage(chatPrefix, "[Songkkaa] " + body);
	}

	private static void sendLocalChatMessage(String message) {
		Minecraft mc = Minecraft.getInstance();
		mc.execute(() -> {
			var player = mc.player;
			if (player != null) {
				player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
			}
		});
	}

	private static void sendServerChatMessage(String chatPrefix, String message) {
		Minecraft mc = Minecraft.getInstance();
		mc.execute(() -> {
			var player = mc.player;
			if (player != null && player.connection != null) {
				String cleanBody = sanitizeForServerChat(message);
				boolean isHypixel = mc.getCurrentServer() != null && mc.getCurrentServer().ip != null && mc.getCurrentServer().ip.toLowerCase().contains("hypixel");
				if (isHypixel && chatPrefix != null && !chatPrefix.trim().isEmpty()) {
					String cmd = chatPrefix.replace("/", "").trim() + " " + cleanBody;
					player.connection.sendCommand(cmd);
				} else {
					player.connection.sendChat(cleanBody);
				}
			}
		});
	}

	private static String sanitizeForServerChat(String msg) {
		StringBuilder sb = new StringBuilder();
		for (char c : msg.toCharArray()) {
			if (c != '§' && c >= ' ' && c != 127) {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}