package com.songgka.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

	private static final Path CONFIG_FILE = Path.of("config", "songkkaa_config.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static ModConfig INSTANCE = new ModConfig();

	public String messageFormat = "{title} - {artist}";
	public String musicProvider = "None"; // "None", "YTM", "Spotify", "Deezer"
	public int ytmPort = 9863;
	public int deezerPort = 9865;
	public String authToken = null;
	public boolean enableAc = true; // /ac ou /a (All Chat / Public Hypixel)
	public boolean enableGc = true; // /gc ou /g (Guild Chat)
	public boolean enablePc = true; // /pc ou /p (Party Chat)
	public boolean enableSong = true; // commande !song activée
	public boolean enableMeow = true; // commande !meow activée
	public boolean enableWanted = true;
	public boolean enableKiss = true;
	public boolean enableFeed = true;
	public boolean enablePoke = true;
	public boolean enablePat = true;
	public boolean enableHug = true;
	public boolean enableSus = true;
	public boolean enableRizz = true;
	public boolean enableJerry = true;
	public boolean enableSoraka = true;
	public boolean enableIq = true;
	public boolean enableSleep = true;
	public boolean enableYuri = true;
	public boolean enableFrieren = true;
	public boolean enableNameColor = true; // couleur du pseudo activée
	public String customPrefix = "";
	public String customSuffix = "";
	public float playerSizeX = 1.0f;
	public float playerSizeY = 1.0f;
	public float playerSizeZ = 1.0f;
	public boolean playerSizeEnabled = true;
	public boolean enableCustomBlocksF7M7 = true;
	public boolean enableLagTimeLost = true;
	public int lagHudX = 10;
	public int lagHudY = 10;
	public float lagHudScale = 1.0f;
	public boolean enableAutoUpdate = true;
	public String customHexColor = "#FF55AA"; // couleur Hex personnalisée (ex: #FF55AA)
	public String customHexColor2 = "#55FFFF"; // deuxième couleur pour le dégradé
	public boolean enableGradient = false; // activer le dégradé à 2 couleurs
	public static void load() {
		try {
			if (Files.exists(CONFIG_FILE)) {
				String json = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
				ModConfig loaded = GSON.fromJson(json, ModConfig.class);
				if (loaded != null) {
					INSTANCE = loaded;
				}
				INSTANCE.save();
			} else {
				INSTANCE.save();
			}
		} catch (Exception e) {
			e.printStackTrace();
			INSTANCE = new ModConfig();
		}
	}

	public void save() {
		try {
			Files.createDirectories(CONFIG_FILE.getParent());
			String json = GSON.toJson(this);
			Files.writeString(CONFIG_FILE, json, StandardCharsets.UTF_8);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
