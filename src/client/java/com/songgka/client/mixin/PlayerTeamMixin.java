package com.songgka.client.mixin;

import com.songgka.client.color.NameColorManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTeam.class)
public class PlayerTeamMixin {

    @Inject(method = "formatNameForTeam", at = @At("RETURN"), cancellable = true)
    private static void onFormatNameForTeam(Team team, Component name, CallbackInfoReturnable<MutableComponent> cir) {
        MutableComponent original = cir.getReturnValue();
        if (original != null) {
            Component colored = NameColorManager.colorizeText(original);
            if (colored instanceof MutableComponent mc) {
                cir.setReturnValue(mc);
            } else if (colored != null) {
                cir.setReturnValue(colored.copy());
            }
        }
    }
}
