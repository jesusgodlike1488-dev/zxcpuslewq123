package com.pulse.client.module.modules.player;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {

    public AutoTotem() {
        super("AutoTotem", "Keeps totem of undying in offhand", Category.PLAYER);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        PlayerScreenHandler handler = mc.player.playerScreenHandler;
        int totemSlot = -1;

        for (int i = 9; i <= 44; i++) {
            if (handler.getSlot(i).getStack().getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }

        if (totemSlot == -1) return;

        mc.interactionManager.clickSlot(handler.syncId, totemSlot, 40, SlotActionType.SWAP, mc.player);
    }
}
