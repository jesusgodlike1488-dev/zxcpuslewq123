package com.pulse.client.mixin;

import com.pulse.client.PulseClient;
import com.pulse.client.module.modules.render.NoRender;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    // 1. Убираем тряску экрана (HurtCam)
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        try {
            NoRender nr = (NoRender) PulseClient.getInstance().getModuleManager().getModule(NoRender.class);
            if (nr != null && nr.isEnabled() && nr.hurtCam.getValue()) {
                ci.cancel(); // Отменяем метод = экран не трясется
            }
        } catch (Exception ignored) {}
    }

    // 2. Убираем поп Тотема с экрана
    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
        try {
            NoRender nr = (NoRender) PulseClient.getInstance().getModuleManager().getModule(NoRender.class);

            // Если включен NoRender и предмет, который хочет вылезти на экран - это Тотем
            if (nr != null && nr.isEnabled() && nr.totemPop.getValue()) {
                if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING) {
                    ci.cancel(); // Отменяем анимацию тотема
                }
            }
        } catch (Exception ignored) {}
    }
}