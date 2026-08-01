package com.songgka.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.songgka.client.color.NameColorManager;
import com.songgka.client.config.ModConfig;
import com.songgka.client.util.ISizeableState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class PlayerRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("RETURN"))
    private void onExtractRenderState(Avatar entity, AvatarRenderState renderState, float f, CallbackInfo ci) {
        if (!(renderState instanceof ISizeableState sizeable)) return;
        
        var mc = Minecraft.getInstance();
        boolean isLocalPlayer = mc.player != null && entity.getUUID().equals(mc.player.getUUID());
        String name = entity.getName().getString().toLowerCase();

        float scaleX = 1.0f;
        float scaleY = 1.0f;
        float scaleZ = 1.0f;
        
        // Add toggle check
        if (!ModConfig.INSTANCE.playerSizeEnabled) {
            sizeable.songgka$setScale(1.0f, 1.0f, 1.0f);
            return;
        }

        if (isLocalPlayer) {
            scaleX = ModConfig.INSTANCE.playerSizeX;
            scaleY = ModConfig.INSTANCE.playerSizeY;
            scaleZ = ModConfig.INSTANCE.playerSizeZ;
        } else {
            if (NameColorManager.PLAYER_SIZES.containsKey(name)) {
                float[] sizes = NameColorManager.PLAYER_SIZES.get(name);
                scaleX = sizes[0];
                scaleY = sizes[1];
                scaleZ = sizes[2];
            }
        }

        sizeable.songgka$setScale(scaleX, scaleY, scaleZ);

        if (sizeable.songgka$hasCustomScale() && renderState.nameTagAttachment != null) {
            renderState.nameTagAttachment = new net.minecraft.world.phys.Vec3(
                renderState.nameTagAttachment.x,
                renderState.nameTagAttachment.y * scaleY,
                renderState.nameTagAttachment.z
            );
        }
    }

    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("RETURN"))
    private void onScale(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        if (state instanceof ISizeableState sizeable) {
            if (sizeable.songgka$hasCustomScale()) {
                poseStack.scale(sizeable.songgka$getScaleX(), sizeable.songgka$getScaleY(), sizeable.songgka$getScaleZ());
            }
        }
    }
}
