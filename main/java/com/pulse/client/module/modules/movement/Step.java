package com.pulse.client.module.modules.movement;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import net.minecraft.entity.attribute.EntityAttributes;

public class Step extends Module {

    public final Setting<Double> height = register(new Setting<Double>("Height", 1.0, "Step height in blocks").setRange(1.0, 2.5));

    public Step() {
        super("Step", "Step up blocks without jumping", Category.MOVEMENT);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;
        var attr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (attr == null) return;
        attr.setBaseValue(height.getValue());
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        var attr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (attr == null) return;
        attr.setBaseValue(0.6);
    }
}
