package com.pulse.client.mixin;

import com.pulse.client.gui.MainMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla TitleScreen with our custom MainMenuScreen.
 */
@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true, require = 0)
    private void onInit(CallbackInfo ci) {
        try {
            MinecraftClient.getInstance().setScreen(new MainMenuScreen());
            ci.cancel();
        } catch (Exception ignored) {
            // If anything fails, fall back to vanilla title screen
        }
    }
}
