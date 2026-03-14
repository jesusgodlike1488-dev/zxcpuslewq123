package com.pulse.client.module.modules.movement;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import net.minecraft.entity.Entity;

import java.lang.reflect.Field;

public class Step extends Module {

    private static final float DEFAULT_STEP_HEIGHT = 0.6f;
    private static final Field STEP_HEIGHT_FIELD;

    static {
        Field f = null;
        try {
            // Обфусцированное имя поля в среде разработки — "stepHeight"
            f = Entity.class.getDeclaredField("stepHeight");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        STEP_HEIGHT_FIELD = f;
    }

    public final Setting<Double> height = register(
            new Setting<Double>("Height", 1.0, "Step height in blocks").setRange(1.0, 2.5));

    public Step() {
        super("Step", "Step up blocks without jumping", Category.MOVEMENT);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;
        setStepHeight(mc.player, height.getValue().floatValue());
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        setStepHeight(mc.player, DEFAULT_STEP_HEIGHT);
    }

    private static void setStepHeight(Entity entity, float value) {
        if (STEP_HEIGHT_FIELD == null) return;
        try {
            STEP_HEIGHT_FIELD.set(entity, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}