package com.pulse.client.module.modules.combat;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class TriggerBot extends Module {

    public final Setting<Double> delay = register(new Setting<Double>("Delay", 50.0, "Delay between hits in ms").setRange(0.0, 500.0));

    private long lastHit;

    public TriggerBot() {
        super("TriggerBot", "Attacks entity when crosshair is on it", Category.COMBAT);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.crosshairTarget == null) return;
        if (mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;

        EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
        if (!(hit.getEntity() instanceof PlayerEntity target)) return;
        if (!target.isAlive()) return;
        if (System.currentTimeMillis() - lastHit < delay.getValue()) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastHit = System.currentTimeMillis();
    }
}
