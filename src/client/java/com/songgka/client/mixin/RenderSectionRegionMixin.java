package com.songgka.client.mixin;

import com.songgka.client.features.GhostBlockManager;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSectionRegion.class)
public class RenderSectionRegionMixin {

    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void onGetBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (GhostBlockManager.isGhostBlocksEnabled) {
            BlockState originalState = cir.getReturnValue();
            if (originalState != null && !originalState.isAir()) {
                String blockId = GhostBlockManager.ghostBlocks.get(pos.asLong());
                if (blockId != null) {
                    net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
                        net.minecraft.resources.Identifier.parse(blockId)
                    );
                    cir.setReturnValue(block.defaultBlockState());
                }
            }
        }
    }
}
