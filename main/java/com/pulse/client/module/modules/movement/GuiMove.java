package com.pulse.client.module.modules.movement;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class GuiMove extends Module {

    public final Setting<Boolean> inventoryMove = register(new Setting<>("InventoryMove", true, "Move while inventory or chests are open"));
    public final Setting<Boolean> chatMove      = register(new Setting<>("ChatMove", false, "Move while chat is open"));
    public final Setting<Boolean> sprint        = register(new Setting<>("Sprint", false, "Sprint while moving in GUI"));
    public final Setting<Boolean> jump          = register(new Setting<>("Jump", true, "Jump while moving in GUI"));

    public GuiMove() {
        super("GuiMove", "Move while GUI is open", Category.MOVEMENT);
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.currentScreen == null) return;

        // Если открыт чат и настройка выключена - выходим
        if (mc.currentScreen instanceof ChatScreen && !chatMove.getValue()) {
            return;
        }

        // HandledScreen включает в себя инвентарь, сундуки, печки, шалкеры и т.д.
        // Это лучше, чем проверять только InventoryScreen.
        if (mc.currentScreen instanceof HandledScreen && !inventoryMove.getValue()) {
            return;
        }

        // Массив всех биндов клавиш, которые мы хотим "нажимать" в GUI
        KeyBinding[] keys = new KeyBinding[]{
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey,
                mc.options.sprintKey
        };

        for (KeyBinding key : keys) {
            // Игнорируем прыжок или спринт, если они отключены в настройках модуля
            if (key == mc.options.jumpKey && !jump.getValue()) continue;
            if (key == mc.options.sprintKey && !sprint.getValue()) continue;

            // Получаем код клавиши (учитывает кастомные настройки управления игрока)
            int keyCode = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode();

            // Проверяем, зажата ли кнопка на клавиатуре физически
            boolean isPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyCode);

            // Передаем состояние нажатия самому майнкрафту
            key.setPressed(isPressed);
        }
    }
}