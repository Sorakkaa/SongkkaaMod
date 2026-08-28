package com.songgka.client.mixin;

import com.songgka.client.features.SkyblockDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "render", at = @At("RETURN"), remap = true, require = 0)
    private void onRender(GuiGraphicsExtractor guiGraphics, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.font != null) {
            if (SkyblockDetector.isInF7OrM7) {
                guiGraphics.text(mc.font, "Floor 7 / M7 Detected! (TRUE)", 10, 10, 0x55FF55);
            } else {
                guiGraphics.text(mc.font, "Floor 7 / M7: FALSE", 10, 10, 0xFF5555);
            }
        }
    }
}
