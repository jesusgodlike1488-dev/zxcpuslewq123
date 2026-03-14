package com.pulse.client.mixin;

import com.pulse.client.PulseClient;
import com.pulse.client.event.events.EventRender3D;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ⚠️ MC 1.20.4: сигнатура WorldRenderer.render() отличается от 1.21.x
 * Нет ObjectAllocator и GpuBufferSlice — это API из 1.21+
 */
@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void onRenderTail(MatrixStack matrices,
                              float tickDelta,
                              long limitTime,
                              boolean renderBlockOutline,
                              Camera camera,
                              GameRenderer gameRenderer,
                              LightmapTextureManager lightmapTextureManager,
                              Matrix4f projectionMatrix,
                              CallbackInfo ci) {
        try {
            PulseClient.getInstance().getEventBus().post(new EventRender3D(matrices, tickDelta));
        } catch (Exception ignored) {}
    }
}
