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
import net.minecraft.world.level.block.Blocks;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class GhostBlockManager {
    private static final Path CONFIG_FILE = Path.of("config", "songkkaa_ghostblocks.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // We store positions as Long for easy JSON serialization, and the block ID as the value
    public static Map<Long, String> ghostBlocks = new HashMap<>();
    
    public static KeyMapping addGhostBlockKey;
    public static KeyMapping applyGhostBlocksKey;
    
    // Nouveaux raccourcis pour les zones
    public static KeyMapping setPos1Key;
    public static KeyMapping setPos2Key;
    public static KeyMapping addAreaKey;
    
    public static BlockPos pos1 = null;
    public static BlockPos pos2 = null;
    public static boolean isGhostBlocksEnabled = false;
    public static String currentGhostMaterial = "minecraft:moss_block";

    private static final KeyMapping.Category GHOST_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("songkkaa", "ghost_blocks")
    );

    public static void init() {
        load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("ghostmat")
                .then(ClientCommands.argument("block", StringArgumentType.word())
                    .executes(context -> {
                        String block = StringArgumentType.getString(context, "block");
                        if (!block.contains(":")) {
                            block = "minecraft:" + block;
                        }
                        currentGhostMaterial = block;
                        sendMsg(Minecraft.getInstance(), "§a[Ghost Blocks] Matériau par défaut défini sur : " + block);
                        return 1;
                    })
                )
            );
        });

        addGhostBlockKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.songgka.add_ghost_block",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_H,
                GHOST_CATEGORY
        ));
        
        applyGhostBlocksKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.songgka.apply_ghost_blocks",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_J,
                GHOST_CATEGORY
        ));

        setPos1Key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.songgka.set_pos1",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_LBRACKET, 
                GHOST_CATEGORY
        ));

        setPos2Key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.songgka.set_pos2",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_RBRACKET, 
                GHOST_CATEGORY
        ));

        addAreaKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.songgka.add_area",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_BACKSLASH, 
                GHOST_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (addGhostBlockKey.consumeClick()) {
                if (client.hitResult != null && client.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) client.hitResult;
                    BlockPos pos = blockHit.getBlockPos();
                    long posLong = pos.asLong();
                    if (ghostBlocks.containsKey(posLong)) {
                        ghostBlocks.remove(posLong);
                    } else {
                        ghostBlocks.put(posLong, currentGhostMaterial);
                    }
                    if (isGhostBlocksEnabled && client.levelRenderer != null) {
                        client.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
                    }
                    save();
                }
            }

            while (setPos1Key.consumeClick()) {
                if (client.hitResult != null && client.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    BlockPos pos = ((net.minecraft.world.phys.BlockHitResult) client.hitResult).getBlockPos();
                    if (pos != null) {
                        pos1 = pos;
                    }
                }
            }

            while (setPos2Key.consumeClick()) {
                if (client.hitResult != null && client.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    BlockPos pos = ((net.minecraft.world.phys.BlockHitResult) client.hitResult).getBlockPos();
                    if (pos != null) {
                        pos2 = pos;
                    }
                }
            }

            while (addAreaKey.consumeClick()) {
                if (pos1 != null && pos2 != null) {
                    int minX = Math.min(pos1.getX(), pos2.getX());
                    int minY = Math.min(pos1.getY(), pos2.getY());
                    int minZ = Math.min(pos1.getZ(), pos2.getZ());
                    int maxX = Math.max(pos1.getX(), pos2.getX());
                    int maxY = Math.max(pos1.getY(), pos2.getY());
                    int maxZ = Math.max(pos1.getZ(), pos2.getZ());
                    
                    int count = 0;
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                BlockPos p = new BlockPos(x, y, z);
                                if (!ghostBlocks.containsKey(p.asLong())) {
                                    ghostBlocks.put(p.asLong(), currentGhostMaterial);
                                    count++;
                                }
                            }
                        }
                    }
                    save();
                    if (isGhostBlocksEnabled && client.levelRenderer != null) {
                        client.levelRenderer.setBlocksDirty(minX, minY, minZ, maxX, maxY, maxZ);
                    }
                    pos1 = null;
                    pos2 = null;
                } else {
                    sendMsg(client, "§c[Ghost Blocks] Vous devez définir Pos1 et Pos2 d'abord !");
                }
            }
            
            while (applyGhostBlocksKey.consumeClick()) {
                isGhostBlocksEnabled = !isGhostBlocksEnabled;
                if (client.levelRenderer != null) {
                    client.levelRenderer.allChanged();
                }
            }
        });
    }

    private static void sendMsg(Minecraft client, String msg) {
        if (client.player != null) {
            client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
        }
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
                java.lang.reflect.Type type = new TypeToken<Map<Long, String>>(){}.getType();
                Map<Long, String> loaded = GSON.fromJson(json, type);
                if (loaded != null) {
                    ghostBlocks = loaded;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ghostBlocks = new HashMap<>();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            java.lang.reflect.Type type = new TypeToken<Map<Long, String>>(){}.getType();
            String json = GSON.toJson(ghostBlocks, type);
            Files.writeString(CONFIG_FILE, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
