package com.pulse.client.mixin;

import com.pulse.client.PulseClient;
import com.pulse.client.event.events.EventRender2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InGameHud.class, priority = 900)
public class MixinInGameHud {

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            // 1.0f = позиция на конец текущего тика. Стабильно работает во всех версиях 1.21.x
            PulseClient.getInstance().getEventBus().post(new EventRender2D(context, 1.0f));
        } catch (Exception ignored) {}
    }
}
