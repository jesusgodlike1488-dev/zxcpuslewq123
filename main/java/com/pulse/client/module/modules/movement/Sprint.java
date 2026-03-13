package com.pulse.client.module.modules.movement;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;

public class Sprint extends Module {

    public final Setting<Boolean> omni = register(new Setting<>("OmniSprint", true, "Sprint in all directions"));

    public Sprint() {
        super("Sprint", "Automatically sprint", Category.MOVEMENT);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;
        if (omni.getValue()) {
            mc.player.setSprinting(true);
        } else {
            if (mc.options.forwardKey.isPressed()) {
                mc.player.setSprinting(true);
            }
        }
    }
}
