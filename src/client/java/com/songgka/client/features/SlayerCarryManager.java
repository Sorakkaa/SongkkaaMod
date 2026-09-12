package com.songgka.client.features;

import com.songgka.client.SonggkaClient;
import com.songgka.client.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class SlayerCarryManager {

    public static class CarrySession {
        public String originalName;
        public int totalBosses;
        public int bossesDone;
        public CarrySession(String originalName, int total) {
            this.originalName = originalName;
            this.totalBosses = total;
            this.bossesDone = 0;
        }
    }

    private static java.util.Map<String, CarrySession> trackedPlayers = new java.util.HashMap<>();
    private static java.util.Set<Entity> knownBosses = new java.util.HashSet<>();

    private static int pendingChatDelay = 0;
    private static String pendingChatMessage = null;
    private static String pendingCompletionMessage = null;
    private static int pendingCompletionDelay = 0;
    public static int showSpawnHudTicks = 0;
    public static String lastSpawnedBossOwner = "";

    public static void setTrackedPlayer(String player, int amount) {
        trackedPlayers.put(player.toLowerCase(), new CarrySession(player, amount));
        SonggkaClient.sendLocalChatMessage("§d§l[CarryTracker] §aSlayer carry tracker added for " + player + " (0/" + amount + " bosses).");
    }

    public static void incrementBosses(String playerRaw) {
        String playerLower = playerRaw.toLowerCase();
        CarrySession session = trackedPlayers.get(playerLower);
        if (session != null) {
            session.bossesDone++;
            
            pendingChatMessage = "pc Boss " + session.bossesDone + "/" + session.totalBosses + " done :3";
            pendingChatDelay = 20; // 1 second delay for kill message

            if (session.bossesDone >= session.totalBosses) {
                pendingCompletionMessage = "pc Carry completed for " + session.originalName + " (" + session.totalBosses + "/" + session.totalBosses + ")";
                pendingCompletionDelay = 60; // 3 seconds delay for completion message
                trackedPlayers.remove(playerLower);
            }
        }
    }

    public static void stopTracking() {
        trackedPlayers.clear();
        knownBosses.clear();
        pendingChatMessage = null;
        pendingChatDelay = 0;
        pendingCompletionMessage = null;
        pendingCompletionDelay = 0;
        SonggkaClient.sendLocalChatMessage("§d§l[CarryTracker] §cSlayer carry tracking stopped for everyone.");
    }

    public static void stopTrackingPlayer(String player) {
        if (trackedPlayers.remove(player.toLowerCase()) != null) {
            SonggkaClient.sendLocalChatMessage("§d§l[CarryTracker] §cSlayer carry tracking stopped for " + player + ".");
        } else {
            SonggkaClient.sendLocalChatMessage("§c[CarryTracker] No active tracker found for " + player + ".");
        }
    }

    public static boolean isActualBossEntity(Entity entity) {
        if (trackedPlayers.isEmpty() || knownBosses.isEmpty()) return false;
        
        // Make any hostile mob near the hologram glow
        for (Entity armorStand : knownBosses) {
            if (armorStand.level() != null && entity.level() != null && armorStand.level() == entity.level()) {
                if (armorStand.distanceTo(entity) < 4.0 && entity instanceof net.minecraft.world.entity.LivingEntity && !(entity instanceof net.minecraft.world.entity.player.Player) && !(entity instanceof net.minecraft.world.entity.decoration.ArmorStand)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void onClientTick(Minecraft mc) {
        if (showSpawnHudTicks > 0) {
            showSpawnHudTicks--;
        }

        if (pendingChatDelay > 0) {
            pendingChatDelay--;
            if (pendingChatDelay <= 0 && pendingChatMessage != null) {
                if (mc.player != null && mc.player.connection != null) {
                    mc.player.connection.sendCommand(pendingChatMessage);
                }
                pendingChatMessage = null;
            }
        }

        if (pendingCompletionDelay > 0) {
            pendingCompletionDelay--;
            if (pendingCompletionDelay <= 0 && pendingCompletionMessage != null) {
                if (mc.player != null && mc.player.connection != null) {
                    mc.player.connection.sendCommand(pendingCompletionMessage);
                }
                pendingCompletionMessage = null;
            }
        }

        if (mc.level != null && (!trackedPlayers.isEmpty() || ModConfig.INSTANCE.autoSendBossCoords)) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                String trackedName = getTrackedBossOwner(entity);
                boolean isMine = ModConfig.INSTANCE.autoSendBossCoords && isMyBoss(entity);
                
                if (trackedName != null || isMine) {
                    if (!knownBosses.contains(entity)) {
                        knownBosses.add(entity);
                        
                        if (trackedName != null) {
                            lastSpawnedBossOwner = trackedName;
                            showSpawnHudTicks = 100; // 5 seconds at 20 ticks/sec
                        }
                        
                        if (isMine) {
                            int bx = entity.getBlockX();
                            int by = entity.getBlockY();
                            int bz = entity.getBlockZ();
                            if (mc.player != null && mc.player.connection != null) {
                                mc.player.connection.sendCommand("pc x: " + bx + ", y: " + by + ", z: " + bz + " | Boss here");
                            }
                        }
                    }
                }
            }
            
            // Clean up removed entities (boss died)
            knownBosses.removeIf(boss -> {
                if (!boss.isAlive() || !entityExists(mc, boss)) {
                    String owner = getTrackedBossOwner(boss);
                    if (owner != null) {
                        incrementBosses(owner);
                    }
                    return true;
                }
                return false;
            });
        }
    }

    private static boolean entityExists(Minecraft mc, Entity boss) {
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getId() == boss.getId()) return true;
        }
        return false;
    }

    // Returns the player name if this entity is a tracked boss, null otherwise
    public static String getTrackedBossOwner(Entity entity) {
        if (trackedPlayers.isEmpty() || entity == null || !entity.hasCustomName()) return null;
        String name = entity.getCustomName().getString();
        String nameLower = name.toLowerCase();
        
        for (java.util.Map.Entry<String, CarrySession> entry : trackedPlayers.entrySet()) {
            if (nameLower.contains(entry.getKey() + "'s") || (nameLower.contains("spawned by:") && nameLower.contains(entry.getKey()))) {
                return entry.getValue().originalName;
            }
        }
        return null;
    }
    
    public static boolean isMyBoss(Entity entity) {
        if (entity == null || !entity.hasCustomName()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        String myName = mc.player.getName().getString().toLowerCase();
        String nameLower = entity.getCustomName().getString().toLowerCase();
        return nameLower.contains(myName + "'s") || (nameLower.contains("spawned by:") && nameLower.contains(myName));
    }
}
