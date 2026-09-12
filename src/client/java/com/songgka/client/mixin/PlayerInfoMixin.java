package com.songgka.client.mixin;

import com.mojang.authlib.GameProfile;
import com.songgka.client.color.NameColorManager;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {

    @Shadow public abstract GameProfile getProfile();

    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void onGetTabListDisplayName(CallbackInfoReturnable<Component> cir) {
        Component original = cir.getReturnValue();
        if (original != null) {
            Component colored = NameColorManager.colorizeText(original);
            if (colored != null) {
                cir.setReturnValue(colored);
            }
        } else {
            GameProfile profile = this.getProfile();
            if (profile != null && profile.name() != null && !profile.name().isEmpty()) {
                Component defaultComp = Component.literal(profile.name());
                Component colored = NameColorManager.colorizeText(defaultComp);
                if (colored != null) {
                    cir.setReturnValue(colored);
                }
            }
        }
    }
}
