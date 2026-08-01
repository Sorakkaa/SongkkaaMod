package com.songgka.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "sendCommand", at = @At("HEAD"))
    private void onSendCommand(String command, CallbackInfo ci) {
        com.songgka.client.SonggkaClient.handleOutgoingCommand(command);
    }

    @Inject(method = "sendChat", at = @At("HEAD"))
    private void onSendChat(String message, CallbackInfo ci) {
        com.songgka.client.SonggkaClient.handleOutgoingChatMessage(message);
    }
}