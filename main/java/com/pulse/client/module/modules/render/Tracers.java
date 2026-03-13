package com.pulse.client.module.modules.render;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventRender3D;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import com.pulse.client.util.RenderUtil;
import net.minecraft.entity.player.PlayerEntity;

public class Tracers extends Module {

    public final Setting<Integer> color = register(new Setting<>("Color", 0xFF00FFFF, "Tracer color (ARGB)"));

    public Tracers() {
        super("Tracers", "Draws lines to all nearby players", Category.RENDER);
    }

    @EventHandler
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;
            RenderUtil.drawTracer(event.getMatrixStack(), player, color.getValue(), event.getTickDelta());
        }
    }
}
