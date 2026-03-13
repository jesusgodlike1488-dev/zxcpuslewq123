package com.pulse.client.module.modules.player;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import net.minecraft.util.Hand;

public class AntiAFK extends Module {

    public final Setting<String> mode = register(new Setting<>("Mode", "Rotate", "Rotate, Jump or Swing"));

    private int tick;

    public AntiAFK() {
        super("AntiAFK", "Prevents AFK kick", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        tick = 0;
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;
        tick++;

        switch (mode.getValue()) {
            case "Rotate" -> mc.player.setYaw(mc.player.getYaw() + (tick % 2 == 0 ? 1 : -1));
            case "Jump"   -> {
                if (tick % 40 == 0 && mc.player.isOnGround()) mc.player.jump();
            }
            case "Swing"  -> {
                if (tick % 10 == 0) mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }
}
