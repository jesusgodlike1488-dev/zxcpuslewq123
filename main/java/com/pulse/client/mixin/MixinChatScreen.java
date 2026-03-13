package com.pulse.client.mixin;

import com.pulse.client.PulseClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class MixinChatScreen {

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true, require = 0)
    private void onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
        try {
            if (message.startsWith(".")) {
                PulseClient.getInstance().getCommandManager().dispatch(message.substring(1));
                ci.cancel();
            }
        } catch (Exception ignored) {}
    }
}
