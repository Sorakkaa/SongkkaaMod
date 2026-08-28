package com.songgka.client.mixin;

import com.songgka.client.features.GhostBlockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.renderer.chunk.RenderSectionRegion.class)
public class RenderSectionRegionMixin {

    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void onGetBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (GhostBlockManager.isGhostBlocksEnabled && com.songgka.client.features.SkyblockDetector.isInF7OrM7) {
            BlockState originalState = cir.getReturnValue();
            if (originalState != null && !originalState.isAir()) {
                String ghost = GhostBlockManager.ghostBlocks.get(pos.asLong());
                if (ghost != null) {
                    boolean isGlass = ghost.contains("glass");
                    if (isGlass && !GhostBlockManager.isGlassGhostBlocksEnabled) {
                        return;
                    }
                    net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.Identifier.tryParse(ghost)).map(ref -> ref.value()).orElse(null);
                    if (block != null) {
                        cir.setReturnValue(block.defaultBlockState());
                    }
                }
            }
        }
    }
}
