package com.pulse.client.module.modules.render;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventRender3D;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import com.pulse.client.util.RenderUtil;
import net.minecraft.entity.player.PlayerEntity;

/**
 * ESP – draws bounding boxes around players through walls.
 * Fixed: previously empty (no actual rendering). Now uses RenderUtil.drawEntityBoundingBox.
 */
public class ESP extends Module {

    public final Setting<Integer> color = register(new Setting<>(  "Color", 0xFF00BFFF, "Outline color (ARGB)"));
    public final Setting<Float>   width = register(new Setting<Float>("Width", 1.5f, "Line width").setRange(0.5, 4.0));

    public ESP() {
        super("ESP", "Draws player outlines through walls", Category.RENDER);
    }

    @EventHandler
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;
            RenderUtil.drawEntityBoundingBox(event.getMatrixStack(), player, color.getValue(), event.getTickDelta());
        }
    }
}
