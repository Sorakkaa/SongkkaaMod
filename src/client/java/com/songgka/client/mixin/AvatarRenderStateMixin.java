package com.songgka.client.mixin;

import com.songgka.client.util.ISizeableState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements ISizeableState {
    @Unique
    private float songgka$scaleX = 1.0f;
    @Unique
    private float songgka$scaleY = 1.0f;
    @Unique
    private float songgka$scaleZ = 1.0f;
    @Unique
    private boolean songgka$hasCustomScale = false;

    @Override
    public float songgka$getScaleX() {
        return songgka$scaleX;
    }

    @Override
    public float songgka$getScaleY() {
        return songgka$scaleY;
    }

    @Override
    public float songgka$getScaleZ() {
        return songgka$scaleZ;
    }

    @Override
    public boolean songgka$hasCustomScale() {
        return songgka$hasCustomScale;
    }

    @Override
    public void songgka$setScale(float x, float y, float z) {
        this.songgka$scaleX = x;
        this.songgka$scaleY = y;
        this.songgka$scaleZ = z;
        this.songgka$hasCustomScale = (x != 1.0f || y != 1.0f || z != 1.0f);
    }
}
