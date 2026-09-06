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
    public static BlockPos pos1 = null;
    public static BlockPos pos2 = null;
    
    public static void init() {
        load();
    }

    private static void sendMsg(Minecraft client, String msg) {
        if (client.player != null) {
            client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
        }
    }

    public static void load() {
        try {
            String json = null;
            try (java.io.InputStream in = GhostBlockManager.class.getResourceAsStream("/assets/songkkaa/ghostblocks.json")) {
                if (in != null) {
                    json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            if (json != null && !json.isEmpty()) {
                java.lang.reflect.Type type = new TypeToken<Map<Long, String>>(){}.getType();
                Map<Long, String> loaded = GSON.fromJson(json, type);
                if (loaded != null) {
                    ghostBlocks = loaded;
                    rebuildChunkMap();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (ghostBlocks == null) ghostBlocks = new HashMap<>();
        }
    }

    private static void rebuildChunkMap() {
        ghostBlocksByChunk.clear();
        for (Map.Entry<Long, String> entry : ghostBlocks.entrySet()) {
            BlockPos pos = BlockPos.of(entry.getKey());
            long chunkPos = (((long)(pos.getX() >> 4)) << 32) | ((long)(pos.getZ() >> 4) & 0xFFFFFFFFL);
            ghostBlocksByChunk.computeIfAbsent(chunkPos, k -> new HashMap<>()).put(entry.getKey(), entry.getValue());
        }
    }

    public static void save() {
        // Disabled saving to bake ghostblocks into the mod
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
        // Sauvegarde et chargement déjà gérés
    }

    public static void restoreBackup() {
        // Sauvegarde et chargement déjà gérés
    }

    public static void setPos1(BlockPos pos) {
        pos1 = pos;
        sendMsg(Minecraft.getInstance(), "§a[GhostBlocks] Pos1 set to " + pos.toShortString());
    }

    public static void setPos2(BlockPos pos) {
        pos2 = pos;
        sendMsg(Minecraft.getInstance(), "§a[GhostBlocks] Pos2 set to " + pos.toShortString());
    }

    public static void setGhostBlockAt(BlockPos pos) {
        if (ghostBlocks.containsKey(pos.asLong())) {
            removeGhostBlockAt(pos);
            return;
        }
        ghostBlocks.put(pos.asLong(), currentGhostMaterial);
        rebuildChunkMap();
        save();
        sendMsg(Minecraft.getInstance(), "§a[GhostBlocks] Placed " + currentGhostMaterial + " at " + pos.toShortString());
        Minecraft.getInstance().levelRenderer.allChanged();
    }

    public static net.minecraft.world.level.block.state.BlockState getGhostBlockVisualState(net.minecraft.world.level.block.state.BlockState original, String ghostMaterial) {
        if (ghostMaterial.equals("minecraft:air")) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        
        net.minecraft.world.level.block.Block baseGhostBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.Identifier.tryParse(ghostMaterial)).map(ref -> ref.value()).orElse(null);
        if (baseGhostBlock == null) return original;
        
        if (original.getBlock() instanceof net.minecraft.world.level.block.StairBlock && baseGhostBlock instanceof net.minecraft.world.level.block.StairBlock) {
            try {
                return baseGhostBlock.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.StairBlock.FACING, original.getValue(net.minecraft.world.level.block.StairBlock.FACING))
                    .setValue(net.minecraft.world.level.block.StairBlock.HALF, original.getValue(net.minecraft.world.level.block.StairBlock.HALF))
                    .setValue(net.minecraft.world.level.block.StairBlock.SHAPE, original.getValue(net.minecraft.world.level.block.StairBlock.SHAPE))
                    .setValue(net.minecraft.world.level.block.StairBlock.WATERLOGGED, original.getValue(net.minecraft.world.level.block.StairBlock.WATERLOGGED));
            } catch (Exception e) { return baseGhostBlock.defaultBlockState(); }
        }
        
        if (original.getBlock() instanceof net.minecraft.world.level.block.SlabBlock && baseGhostBlock instanceof net.minecraft.world.level.block.SlabBlock) {
            try {
                return baseGhostBlock.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, original.getValue(net.minecraft.world.level.block.SlabBlock.TYPE))
                    .setValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED, original.getValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED));
            } catch (Exception e) { return baseGhostBlock.defaultBlockState(); }
        }

        if (original.getBlock() instanceof net.minecraft.world.level.block.FenceBlock && baseGhostBlock instanceof net.minecraft.world.level.block.FenceBlock) {
            try {
                return baseGhostBlock.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.FenceBlock.NORTH, original.getValue(net.minecraft.world.level.block.FenceBlock.NORTH))
                    .setValue(net.minecraft.world.level.block.FenceBlock.SOUTH, original.getValue(net.minecraft.world.level.block.FenceBlock.SOUTH))
                    .setValue(net.minecraft.world.level.block.FenceBlock.EAST, original.getValue(net.minecraft.world.level.block.FenceBlock.EAST))
                    .setValue(net.minecraft.world.level.block.FenceBlock.WEST, original.getValue(net.minecraft.world.level.block.FenceBlock.WEST))
                    .setValue(net.minecraft.world.level.block.FenceBlock.WATERLOGGED, original.getValue(net.minecraft.world.level.block.FenceBlock.WATERLOGGED));
            } catch (Exception e) { return baseGhostBlock.defaultBlockState(); }
        }

        return baseGhostBlock.defaultBlockState();
    }

    public static void removeGhostBlockAt(BlockPos pos) {
        if (ghostBlocks.containsKey(pos.asLong())) {
            ghostBlocks.remove(pos.asLong());
            rebuildChunkMap();
            save();
            sendMsg(Minecraft.getInstance(), "§c[GhostBlocks] Removed ghost block at " + pos.toShortString());
            Minecraft.getInstance().levelRenderer.allChanged();
        } else {
            sendMsg(Minecraft.getInstance(), "§e[GhostBlocks] No ghost block found at " + pos.toShortString());
        }
    }

    public static void fillZone() {
        if (pos1 == null || pos2 == null) {
            sendMsg(Minecraft.getInstance(), "§c[GhostBlocks] Pos1 or Pos2 is not set!");
            return;
        }
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        int count = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    ghostBlocks.put(BlockPos.asLong(x, y, z), currentGhostMaterial);
                    count++;
                }
            }
        }
        rebuildChunkMap();
        save();
        sendMsg(Minecraft.getInstance(), "§a[GhostBlocks] Filled " + count + " blocks with " + currentGhostMaterial);
        Minecraft.getInstance().levelRenderer.allChanged();
    }

    public static void clearZone() {
        if (pos1 == null || pos2 == null) {
            sendMsg(Minecraft.getInstance(), "§c[GhostBlocks] Pos1 or Pos2 is not set!");
            return;
        }
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        int count = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    long key = BlockPos.asLong(x, y, z);
                    if (ghostBlocks.containsKey(key)) {
                        ghostBlocks.remove(key);
                        count++;
                    }
                }
            }
        }
        rebuildChunkMap();
        save();
        sendMsg(Minecraft.getInstance(), "§c[GhostBlocks] Cleared " + count + " ghost blocks from zone.");
        Minecraft.getInstance().levelRenderer.allChanged();
    }
}
