package com.songgka.client.features;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class FrierenScreamerManager {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("songkkaa", "textures/gui/frieren_scream.png");
    private static long endTime = 0;

    public static void trigger() {
        endTime = System.currentTimeMillis() + 2500; // Match 2.4s voice duration

        // Play the authentic Frieren scream voice audio asynchronously
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                java.io.InputStream is = FrierenScreamerManager.class.getResourceAsStream("/assets/songkkaa/sounds/frieren_scream.wav");
                if (is == null) {
                    is = FrierenScreamerManager.class.getResourceAsStream("/assets/songgka/sounds/frieren_scream.wav");
                }
                if (is != null) {
                    BufferedInputStream bis = new BufferedInputStream(is);
                    AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
                    Clip clip = AudioSystem.getClip();
                    clip.open(ais);
                    if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                        gainControl.setValue(-15.0f);
                    }
                    clip.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static boolean isActive() {
        return System.currentTimeMillis() < endTime;
    }
}
