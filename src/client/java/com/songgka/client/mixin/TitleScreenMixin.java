package com.songgka.client.mixin;

import com.songgka.client.update.UpdateManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("RETURN"))
    private void onExtractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (UpdateManager.updateReady) {
            String msg = "§a[Songgka] Update (v" + UpdateManager.downloadedVersion + ") downloaded!";
            String msg2 = "§ePlease restart the game and delete the old JAR in your mods folder.";
            
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int width = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
            
            // Make it blink
            if (System.currentTimeMillis() % 1000 < 500) {
                guiGraphics.text(
                    font, 
                    msg, 
                    (width - font.width(msg)) / 2, 
                    10, 
                    0xFFFFFF
                );
            }
            guiGraphics.text(
                font, 
                msg2, 
                (width - font.width(msg2)) / 2, 
                22, 
                0xFFFFFF
            );
        }
    }
}
