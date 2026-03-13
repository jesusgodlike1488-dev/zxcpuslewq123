package com.pulse.client.mixin;

import com.pulse.client.PulseClient;
import com.pulse.client.module.modules.render.ESP;
import com.pulse.client.module.modules.render.ItemESP;
import com.pulse.client.util.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {

    /**
     * Блокируем мышь в Focus режиме.
     * Без этого мышь двигает yaw 60+ раз/сек между тиками → дёрганье.
     */
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void onChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        try {
            Entity self = (Entity) (Object) this;
            if (self != MinecraftClient.getInstance().player) return;

            if (RotationUtil.active && RotationUtil.focusMode.equalsIgnoreCase("Focus")) {
                ci.cancel();
            }
        } catch (Exception ignored) {}
    }

    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true, require = 0)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        try {
            Entity self = (Entity)(Object)this;
            MinecraftClient mc = MinecraftClient.getInstance();

            if (self instanceof PlayerEntity && self != mc.player) {
                ESP esp = PulseClient.getInstance().getModuleManager().getModule(ESP.class);
                if (esp != null && esp.isEnabled()) {
                    cir.setReturnValue(true);
                    return;
                }
            }

            if (self instanceof ItemEntity) {
                ItemESP itemEsp = (ItemESP) PulseClient.getInstance().getModuleManager().getModule(ItemESP.class);
                if (itemEsp != null && itemEsp.isEnabled()) {
                    cir.setReturnValue(true);
                }
            }
        } catch (Exception ignored) {}
    }

    @Inject(method = "getTeamColorValue", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        try {
            Entity self = (Entity)(Object)this;
            if (!(self instanceof PlayerEntity)) return;
            if (self == MinecraftClient.getInstance().player) return;

            ESP esp = PulseClient.getInstance().getModuleManager().getModule(ESP.class);
            if (esp != null && esp.isEnabled()) {
                cir.setReturnValue(esp.color.getValue() & 0x00FFFFFF);
            }
        } catch (Exception ignored) {}
    }
}