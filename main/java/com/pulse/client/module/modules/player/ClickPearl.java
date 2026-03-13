package com.pulse.client.module.modules.player;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

/**
 * ClickPearl — кидает эндер-перл по нажатию СКМ.
 *
 * Логика:
 * 1. СКМ нажата → ищем перл в инвентаре
 * 2. Свапаем перл в руку (слот 40 = offhand, мгновенный swap)
 * 3. Кидаем перл из offhand
 * 4. Свапаем обратно что было в offhand
 *
 * Работает из любого слота инвентаря (9-44).
 */
public class ClickPearl extends Module {

    private boolean wasPressed = false;
    private boolean needSwapBack = false;
    private int pearlSlot = -1;

    public ClickPearl() {
        super("ClickPearl", "СКМ кидает эндер-перл из инвентаря", Category.PLAYER);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;

        // Возвращаем предмет обратно после броска
        if (needSwapBack && pearlSlot != -1) {
            mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    pearlSlot, 40, SlotActionType.SWAP, mc.player);
            needSwapBack = false;
            pearlSlot = -1;
            return;
        }

        boolean pressed = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;

        if (pressed && !wasPressed) {
            throwPearl();
        }

        wasPressed = pressed;
    }

    private void throwPearl() {
        // Проверяем: может перл уже в основной руке?
        if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            return;
        }

        // Или в offhand?
        if (mc.player.getOffHandStack().getItem() == Items.ENDER_PEARL) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            return;
        }

        // Ищем перл в инвентаре (слоты 9-44 в playerScreenHandler)
        int slot = findPearl();
        if (slot == -1) return;

        // Свапаем перл в offhand (слот 40)
        mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                slot, 40, SlotActionType.SWAP, mc.player);

        // Кидаем из offhand
        mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);

        // Запоминаем слот для обратного свапа в следующем тике
        pearlSlot = slot;
        needSwapBack = true;
    }

    private int findPearl() {
        // Сначала хотбар (слоты 36-44), потом основной инвентарь (9-35)
        for (int i = 36; i <= 44; i++) {
            if (mc.player.playerScreenHandler.getSlot(i).getStack().getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }
        for (int i = 9; i <= 35; i++) {
            if (mc.player.playerScreenHandler.getSlot(i).getStack().getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
        wasPressed = false;
        needSwapBack = false;
        pearlSlot = -1;
    }
}