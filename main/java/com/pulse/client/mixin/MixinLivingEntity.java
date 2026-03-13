package com.pulse.client.mixin;

import com.pulse.client.PulseClient;
import com.pulse.client.module.modules.movement.NoSlow;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "isUsingItem", at = @At("RETURN"), cancellable = true, require = 0)
    private void onIsUsingItem(CallbackInfoReturnable<Boolean> cir) {
        try {
            LivingEntity self = (LivingEntity)(Object)this;
            NoSlow noSlow = PulseClient.getInstance().getModuleManager().getModule(NoSlow.class);
            if (noSlow == null || !noSlow.isEnabled() || !noSlow.items.getValue()) return;
            if (!(self instanceof net.minecraft.client.network.ClientPlayerEntity)) return;
            cir.setReturnValue(false);
        } catch (Exception ignored) {}
    }
}
