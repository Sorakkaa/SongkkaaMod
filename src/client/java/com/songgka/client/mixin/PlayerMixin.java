package com.songgka.client.mixin;

import com.songgka.client.color.NameColorManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void onGetDisplayName(CallbackInfoReturnable<Component> cir) {
        net.minecraft.world.entity.Entity entity = (net.minecraft.world.entity.Entity) (Object) this;
        Player player = (Player) (Object) this;
        UUID uuid = entity.getUUID();
        String name = player.getScoreboardName();

        Component original = cir.getReturnValue();
        if (original != null) {
            Component colored = NameColorManager.colorizeText(original);
            if (colored != null) {
                cir.setReturnValue(colored);
            }
        }
    }
}
