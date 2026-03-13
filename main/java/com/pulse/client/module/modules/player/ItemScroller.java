package com.pulse.client.module.modules.player;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class ItemScroller extends Module {

    // Задержка 80-100 мс — идеальна для обхода античита FunAC (кики за FastClick исключены)
    public final Setting<Integer> delay = register(
            new Setting<>("Delay", 80, "Задержка (мс)").setRange(0, 300));

    private final Set<Integer> clickedSlots = new HashSet<>();
    private long lastClickTime = 0;

    // Кеш для рефлексии (сохраняет стабильный FPS)
    private Field focusedSlotField = null;

    public ItemScroller() {
        super("ItemScroller", "Быстро перекладывает вещи при зажатом ЛКМ", Category.PLAYER);
    }

    @Override
    public void onDisable() {
        clickedSlots.clear();
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.interactionManager == null) return;

        // Работаем только если открыт инвентарь или сундук
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
            clickedSlots.clear();
            return;
        }

        // Проверяем, зажата ли только левая кнопка мыши (ЛКМ)
        boolean isMouseDown = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;

        // Если отпустили кнопку — сбрасываем память слотов
        if (!isMouseDown) {
            clickedSlots.clear();
            return;
        }

        // УМНАЯ ПРОВЕРКА: Если в курсоре уже есть предмет (игрок взял его и несет),
        // мы не перекладываем предметы, чтобы дать возможность раскладывать вещи в верстаке.
        if (!screen.getScreenHandler().getCursorStack().isEmpty()) {
            clickedSlots.clear();
            return;
        }

        // Получаем слот под мышкой через рефлексию
        Slot hoveredSlot = getHoveredSlot(screen);

        // Если слота нет, он пустой, или мы его уже кликнули за этот "свайп" — пропускаем
        if (hoveredSlot == null || !hoveredSlot.hasStack() || clickedSlots.contains(hoveredSlot.id)) {
            return;
        }

        // Античит проверка (Таймер)
        if (System.currentTimeMillis() - lastClickTime < delay.getValue()) {
            return; // Ждем кулдаун
        }

        // Выполняем клик (эмуляция Shift + ЛКМ, хотя Shift мы не держим)
        mc.interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                hoveredSlot.id,
                0,
                SlotActionType.QUICK_MOVE,
                mc.player
        );

        // Запоминаем слот и сбрасываем таймер
        clickedSlots.add(hoveredSlot.id);
        lastClickTime = System.currentTimeMillis();
    }

    /**
     * Безопасное получение слота под мышкой (без Mixin)
     */
    private Slot getHoveredSlot(HandledScreen<?> screen) {
        if (focusedSlotField == null) {
            try {
                // В среде разработки (IDEA)
                focusedSlotField = HandledScreen.class.getDeclaredField("focusedSlot");
            } catch (NoSuchFieldException e1) {
                try {
                    // В скомпилированном клиенте
                    focusedSlotField = HandledScreen.class.getDeclaredField("field_2787");
                } catch (NoSuchFieldException e2) {
                    // Универсальный запасной вариант
                    for (Field field : HandledScreen.class.getDeclaredFields()) {
                        if (field.getType() == Slot.class) {
                            focusedSlotField = field;
                            break;
                        }
                    }
                }
            }
            if (focusedSlotField != null) {
                focusedSlotField.setAccessible(true);
            }
        }

        if (focusedSlotField != null) {
            try {
                return (Slot) focusedSlotField.get(screen);
            } catch (IllegalAccessException ignored) {}
        }
        return null;
    }
}