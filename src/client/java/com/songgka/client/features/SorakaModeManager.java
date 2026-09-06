package com.songgka.client.features;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class SorakaModeManager {
    private static long endTimestamp = 0;
    public static final Identifier SKIN_ID = Identifier.fromNamespaceAndPath("songkkaa", "entity/soraka");
    public static final Identifier SKIN_LOCATION = Identifier.fromNamespaceAndPath("songkkaa", "textures/entity/soraka.png");

    private static final Map<Entity, RemotePlayer> dummyMap = new WeakHashMap<>();

    public static void activate() {
        JerryModeManager.deactivate();
        endTimestamp = System.currentTimeMillis() + 10000;
    }

    public static void deactivate() {
        endTimestamp = 0;
        dummyMap.clear();
    }

    public static boolean isActive() {
        if (System.currentTimeMillis() >= endTimestamp) {
            dummyMap.clear();
            return false;
        }
        return true;
    }

    public static RemotePlayer getDummyPlayer(Entity original) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        RemotePlayer dummy = dummyMap.get(original);
        if (dummy == null || dummy.level() != mc.level) {
            dummy = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "Soraka"));
            dummyMap.put(original, dummy);
        }
        return dummy;
    }
}


