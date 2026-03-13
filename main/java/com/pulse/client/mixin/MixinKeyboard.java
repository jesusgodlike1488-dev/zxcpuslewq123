package com.pulse.client.mixin;

import com.pulse.client.PulseClient;
import com.pulse.client.event.events.EventKey;
import com.pulse.client.module.Module;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard {

    @Inject(method = "onKey", at = @At("HEAD"), require = 0)
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        // 1. Проверяем, что клавиша именно НАЖАТА (а не отпущена или зажата)
        if (action != GLFW.GLFW_PRESS) return;

        // Игнорируем нажатия "без кнопки" (бывает при системных эвентах)
        if (key == GLFW.GLFW_KEY_UNKNOWN || key == 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();

        // 2. Блокируем бинды, если открыт чат, инвентарь или нажат ESC
        if (mc.currentScreen != null) return;

        try {
            // Защита от краша при запуске игры, когда клиент еще не инициализирован
            if (PulseClient.getInstance() == null || PulseClient.getInstance().getModuleManager() == null) {
                return;
            }

            // 3. Открытие ClickGUI на Правый Shift
            if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                mc.setScreen(PulseClient.getInstance().getClickGUI());
                return;
            }

            // 4. Отправка эвента (если он нужен для других систем)
            PulseClient.getInstance().getEventBus().post(new EventKey(key));

            // 5. Включение/Выключение модулей
            for (Module module : PulseClient.getInstance().getModuleManager().getModules()) {
                if (module.getKeybind() == key) {
                    module.toggle();
                }
            }

        } catch (Exception e) {
            // Если тут произойдет ошибка, она ВЫВЕДЕТСЯ В КОНСОЛЬ, а не проглотится!
            System.err.println("[PulseClient] Ошибка при нажатии кнопки: " + key);
            e.printStackTrace();
        }
    }
}