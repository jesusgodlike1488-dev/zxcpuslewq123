package com.pulse.client.mixin;

import com.pulse.client.PulseClient;
import com.pulse.client.module.modules.combat.Reach;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

    @Inject(method = "getReachDistance", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetReachDistance(CallbackInfoReturnable<Float> cir) {
        Reach reach = PulseClient.getInstance().getModuleManager().getModule(Reach.class);
        if (reach != null && reach.isEnabled()) {
            cir.setReturnValue(reach.reach.getValue().floatValue());
        }
    }
}
