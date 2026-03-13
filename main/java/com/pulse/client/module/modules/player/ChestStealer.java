package com.pulse.client.module.modules.player;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class ChestStealer extends Module {

    public final Setting<Double> delay = register(new Setting<Double>("Delay", 50.0, "Delay between steals in ms").setRange(0.0, 500.0));

    private long lastSteal;

    public ChestStealer() {
        super("ChestStealer", "Automatically moves items from chests to inventory", Category.PLAYER);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) return;
        if (System.currentTimeMillis() - lastSteal < delay.getValue()) return;

        int containerSlots = handler.getRows() * 9;
        for (int i = 0; i < containerSlots; i++) {
            if (handler.getSlot(i).getStack().isEmpty()) continue;
            mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            lastSteal = System.currentTimeMillis();
            return;
        }
    }
}
