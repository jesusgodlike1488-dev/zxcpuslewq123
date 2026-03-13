package com.pulse.client.mixin;

import com.pulse.client.PulseClient;
import com.pulse.client.event.events.EventRender3D;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void onRenderTail(ObjectAllocator objectAllocator,
                              RenderTickCounter tickCounter,
                              boolean renderBlockOutline,
                              Camera camera,
                              Matrix4f positionMatrix,
                              Matrix4f projectionMatrix,
                              GpuBufferSlice gpuBufferSlice,
                              Vector4f vector4f,
                              boolean bl,
                              CallbackInfo ci) {
        try {
            MatrixStack stack = new MatrixStack();
            stack.multiplyPositionMatrix(positionMatrix);
            // tickDelta = 1.0f достаточно для Tracers/ESP, избегаем нестабильного API RenderTickCounter
            PulseClient.getInstance().getEventBus().post(new EventRender3D(stack, 1.0f));
        } catch (Exception ignored) {}
    }
}