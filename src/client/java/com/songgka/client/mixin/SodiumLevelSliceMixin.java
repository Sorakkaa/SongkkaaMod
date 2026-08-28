package com.songgka.client.mixin;

import com.songgka.client.features.GhostBlockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public class SodiumLevelSliceMixin {

    @Inject(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetBlockState(int x, int y, int z, CallbackInfoReturnable<BlockState> cir) {
        if (GhostBlockManager.isGhostBlocksEnabled && com.songgka.client.features.SkyblockDetector.isInF7OrM7) {
            BlockState originalState = cir.getReturnValue();
            if (originalState != null && !originalState.isAir()) {
                BlockPos pos = new BlockPos(x, y, z);
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
