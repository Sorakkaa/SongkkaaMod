package com.songgka.client.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class GhostBlockManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // We store positions as Long for easy JSON serialization, and the block ID as the value
    public static Map<Long, String> ghostBlocks = new HashMap<>();
    public static Map<Long, Map<Long, String>> ghostBlocksByChunk = new HashMap<>();
    public static boolean isGhostBlocksEnabled = true;
    public static boolean isGlassGhostBlocksEnabled = true;
    public static String currentGhostMaterial = "minecraft:moss_block";
    
    public static void init() {
        load();
    }

    private static void sendMsg(Minecraft client, String msg) {
        if (client.player != null) {
            client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
        }
    }

    public static void load() {
        try (java.io.InputStream in = GhostBlockManager.class.getResourceAsStream("/assets/songkkaa/ghostblocks.json")) {
            if (in != null) {
                String json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                java.lang.reflect.Type type = new TypeToken<Map<Long, String>>(){}.getType();
                Map<Long, String> loaded = GSON.fromJson(json, type);
                if (loaded != null) {
                    ghostBlocks = loaded;
                    ghostBlocksByChunk.clear();
                    for (Map.Entry<Long, String> entry : ghostBlocks.entrySet()) {
                        BlockPos pos = BlockPos.of(entry.getKey());
                        long chunkPos = (((long)(pos.getX() >> 4)) << 32) | ((long)(pos.getZ() >> 4) & 0xFFFFFFFFL);
                        ghostBlocksByChunk.computeIfAbsent(chunkPos, k -> new HashMap<>()).put(entry.getKey(), entry.getValue());
                    }
                }
            } else {
                System.out.println("[GhostBlocks] Could not find /assets/songkkaa/ghostblocks.json in resources!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ghostBlocks = new HashMap<>();
        }
    }

    public static void save() {
        // Sauvegarde désactivée (les blocs sont intégrés au mod en lecture seule)
    }

    public static void setCurrentGhostMaterial(String material) {
        currentGhostMaterial = material;
    }

    public static void changeAllGhostBlocks(String newMaterial) {
        String oldMaterial = currentGhostMaterial;
        currentGhostMaterial = newMaterial;
        for (Map.Entry<Long, String> entry : ghostBlocks.entrySet()) {
            if (entry.getValue().equals(oldMaterial)) {
                entry.setValue(newMaterial);
            }
        }
        save();
        Minecraft client = Minecraft.getInstance();
        if (isGhostBlocksEnabled && client.levelRenderer != null) {
            client.levelRenderer.allChanged();
        }
    }

    public static void createBackup() {
        // Fonctionnalité désactivée
    }

    public static void restoreBackup() {
        // Fonctionnalité désactivée
    }
}
