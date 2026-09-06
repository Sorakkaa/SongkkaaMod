package com.songgka.client.mixin;

import com.songgka.client.features.FrierenScreamerManager;
import com.songgka.client.features.SkyblockDetector;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onExtractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.font != null) {
            if (SkyblockDetector.isInF7OrM7) {
                guiGraphics.text(mc.font, "Floor 7 / M7 Detected! (TRUE)", 10, 10, 0x55FF55);
            } else {
                guiGraphics.text(mc.font, "Floor 7 / M7: FALSE", 10, 10, 0xFF5555);
            }
          if (com.songgka.client.config.ModConfig.INSTANCE.enableLagTimeLost) {
            if (com.songgka.client.features.DungeonLagTracker.inRun) {
                long ms = 0;
                long now = System.currentTimeMillis();
                long lastScoreboard = com.songgka.client.features.DungeonLagTracker.lastScoreboardChangeRealTime;
                boolean isCurrentlyLagging = false;
                
                if (lastScoreboard > 0) {
                    long timeSinceUpdate = now - lastScoreboard;
                    if (timeSinceUpdate > 1500) { // 1500ms means 500ms of lag
                        ms = timeSinceUpdate - 1000;
                        isCurrentlyLagging = true;
                    }
                }
                
                if (isCurrentlyLagging) {
                    String text = ms + "ms";
                    int x = com.songgka.client.config.ModConfig.INSTANCE.lagHudX;
                    int y = com.songgka.client.config.ModConfig.INSTANCE.lagHudY;
                    float scale = com.songgka.client.config.ModConfig.INSTANCE.lagHudScale;
                    if (scale <= 0.1f) scale = 1.0f;
                    
                    guiGraphics.pose().pushMatrix();
                    guiGraphics.pose().translate(x, y);
                    guiGraphics.pose().scale(scale, scale);
                    guiGraphics.text(mc.font, text, 0, 0, 0xFFFF5555);
                    guiGraphics.pose().popMatrix();
                }
            }
        }
        }

        if (com.songgka.client.features.FrierenScreamerManager.isActive()) {
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();

            // Dark jumpscare backdrop
            guiGraphics.fill(0, 0, sw, sh, 0xEE110000);

            // Screen shake
            long now = System.currentTimeMillis();
            int shakeX = (int) ((now * 47) % 11) - 5;
            int shakeY = (int) ((now * 31) % 11) - 5;

            int imgSize = Math.min(sw, sh) - 40;
            if (imgSize < 50) imgSize = Math.min(sw, sh);
            int imgX = (sw - imgSize) / 2 + shakeX;
            int imgY = (sh - imgSize) / 2 + shakeY;

            guiGraphics.blit(com.songgka.client.features.FrierenScreamerManager.TEXTURE, imgX, imgY, imgX + imgSize, imgY + imgSize, 0.0f, 1.0f, 0.0f, 1.0f);
        }
    }
}
