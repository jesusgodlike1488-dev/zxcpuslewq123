package com.pulse.client.gui;

import com.pulse.client.PulseClient;
import com.pulse.client.gui.font.FontManager;
import com.pulse.client.module.Category;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {

    private final List<CategoryPanel> panels = new ArrayList<>();

    // Состояние поиска
    private String searchText = "";
    private boolean searchFocused = false;
    private long cursorBlink = 0;

    // Размеры строки поиска
    private static final int SEARCH_W = 160;
    private static final int SEARCH_H = 20;

    public ClickGUI() {
        super(Text.literal("PulseClient – GUI"));
    }

    // ─────────────────────────────── INIT ───────────────────────────────── //

    @Override
    protected void init() {
        panels.clear();

        int panelW   = 150; // Ширина панели из CategoryPanel
        int panelGap = 10;  // Расстояние между панелями
        int numCats  = Category.values().length;

        // Центрируем панели по горизонтали
        int totalW   = numCats * panelW + (numCats - 1) * panelGap;
        int startX   = (width - totalW) / 2;
        int startY   = 30; // Отступ сверху

        int i = 0;
        for (Category cat : Category.values()) {
            var modules = PulseClient.getInstance().getModuleManager().getModulesByCategory(cat);
            if (!modules.isEmpty()) {
                panels.add(new CategoryPanel(cat, modules, startX + i * (panelW + panelGap), startY));
                i++;
            }
        }
    }

    // ─────────────────────────────── RENDER ─────────────────────────────── //

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Темный полупрозрачный фон на весь экран
        context.fill(0, 0, width, height, 0xAA000000);

        // Рендер самих панелей (передаем текст поиска для фильтрации)
        for (CategoryPanel panel : panels) {
            panel.render(context, mouseX, mouseY, searchText);
        }

        // Рендер строки поиска
        renderSearchBar(context, mouseX, mouseY);

        // Вотермарка в левом верхнем углу
        FontManager.SMALL.drawStringWithShadow(context,
                "PulseClient  |  Right Shift to close", 4, 4, 0xFF555555);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderSearchBar(DrawContext context, int mx, int my) {
        int sx = (width  - SEARCH_W) / 2;
        int sy = height - SEARCH_H - 15;

        boolean hovered = mx >= sx && mx <= sx + SEARCH_W && my >= sy && my <= sy + SEARCH_H;

        // Фон строки поиска (стандартные ровные прямоугольники)
        int bgCol = searchFocused ? 0xFF181824 : (hovered ? 0xFF20202E : 0xFF111115);
        context.fill(sx, sy, sx + SEARCH_W, sy + SEARCH_H, bgCol);

        // Обводка строки поиска (нижняя линия)
        int borderCol = searchFocused ? 0xFF4A90E2 : 0xFF333344;
        context.fill(sx, sy + SEARCH_H - 1, sx + SEARCH_W, sy + SEARCH_H, borderCol);

        // Текст поиска
        String display = searchText.isEmpty() && !searchFocused ? "Поиск..." : searchText;
        int    textCol = searchText.isEmpty() && !searchFocused ? 0xFF666666 : 0xFFCCCCCC;

        // Мигающий курсор
        if (searchFocused) {
            long now = System.currentTimeMillis();
            if ((now - cursorBlink) / 500 % 2 == 0) {
                display = display + "_";
            }
        }

        FontManager.REGULAR.drawString(context, display,
                sx + 8, sy + (SEARCH_H - FontManager.REGULAR.getHeight()) / 2f, textCol);
    }

    // ─────────────────────────── ИВЕНТЫ МЫШИ (Здесь была ошибка!) ───────────────────────────── //

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int sx = (width  - SEARCH_W) / 2;
        int sy = height - SEARCH_H - 15;

        // Фокус на строку поиска
        if (mx >= sx && mx <= sx + SEARCH_W && my >= sy && my <= sy + SEARCH_H) {
            searchFocused = true;
            cursorBlink   = System.currentTimeMillis();
            return true;
        } else {
            searchFocused = false;
        }

        // ПЕРЕДАЧА КЛИКОВ В ПАНЕЛИ
        for (CategoryPanel panel : panels) {
            if (panel.mouseClicked(mx, my, button)) {
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        for (CategoryPanel panel : panels) {
            panel.mouseDragged(mx, my);
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        for (CategoryPanel panel : panels) {
            panel.mouseReleased();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hDelta, double vDelta) {
        for (CategoryPanel panel : panels) {
            if (panel.mouseScrolled(mx, my, vDelta)) return true;
        }
        return super.mouseScrolled(mx, my, hDelta, vDelta);
    }

    // ─────────────────────────── ИВЕНТЫ КЛАВИАТУРЫ ───────────────────────────── //

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 1. Сначала спрашиваем панели: "Кто-то биндит модуль?"
        for (CategoryPanel panel : panels) {
            if (panel.keyPressed(keyCode, scanCode, modifiers)) {
                return true; // Панель забиндила кнопку, выходим!
            }
        }

        // 2. Логика строки поиска
        if (searchFocused) {
            if (keyCode == 259) { // Backspace
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
                return true;
            }
            if (keyCode == 256) { // ESC
                if (!searchText.isEmpty()) {
                    searchText = "";
                    return true;
                }
                searchFocused = false;
                return true;
            }
        }

        // 3. Закрытие GUI по умолчанию
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && chr >= 32 && searchText.length() < 30) {
            searchText += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause()        { return false; }

    @Override
    public boolean shouldCloseOnEsc()   { return true; }
}