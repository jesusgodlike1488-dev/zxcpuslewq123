package com.pulse.client.gui;

import com.pulse.client.gui.font.FontManager;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CategoryPanel {

    // --- ОРИГИНАЛЬНЫЕ РАЗМЕРЫ (чтобы клики работали идеально) ---
    private static final int W          = 150;
    private static final int HEADER_H   = 24;
    private static final int MODULE_H   = 20;
    private static final int SETTING_H  = 20;
    private static final int DOTS_W     = 18;

    // --- ПРЕМИАЛЬНЫЕ ЦВЕТА (Стиль Meteor / Modern Dark) ---
    private static final int C_BG         = 0xF0111115; // Глубокий, почти непрозрачный фон (без мыла)
    private static final int C_HEADER     = 0xFF181820; // Фон заголовка
    private static final int C_ACCENT     = 0xFF6C7A89; // Цвет акцента по умолчанию (если хочешь синий - 0xFF4A90E2)
    private static final int C_ACCENT_ON  = 0xFF4A90E2; // Ярко-синий для включенных модулей
    private static final int C_HOVER      = 0x1AFFFFFF; // Легкая подсветка при наведении мыши
    private static final int C_SETTING_BG = 0xFF0D0D12; // Темный фон для настроек
    private static final int C_TEXT_ON    = 0xFFFFFFFF; // Белый текст
    private static final int C_TEXT_OFF   = 0xFFAAAAAA; // Серый текст

    private final Category cat;
    private final List<Module> modules;
    private float x, y;
    private boolean collapsed = false;
    private boolean dragging  = false;
    private float dox, doy;

    private int openSettings = -1;
    private int sliderModule = -1;
    private int sliderSetting = -1;
    private boolean sliderActive = false;
    private Module bindingModule = null;

    public CategoryPanel(Category cat, List<Module> modules, float x, float y) {
        this.cat = cat;
        this.modules = modules;
        this.x = x;
        this.y = y;
    }

    // ================================= ВИЗУАЛ ================================= //

    public void render(DrawContext ctx, int mx, int my, String filter) {
        List<Module> vis = filtered(filter);

        int bodyH = 0;
        if (!collapsed) {
            for (int i = 0; i < vis.size(); i++) {
                bodyH += MODULE_H;
                if (i == openSettings) {
                    bodyH += vis.get(i).getSettings().size() * SETTING_H;
                }
            }
        }
        int totalH = HEADER_H + bodyH;

        // 1. Внешняя тень (имитация через полупрозрачный бордер)
        ctx.fill((int)x - 1, (int)y - 1, (int)(x + W + 1), (int)(y + totalH + 1), 0x40000000);

        // 2. Основной фон
        ctx.fill((int)x, (int)y, (int)(x + W), (int)(y + totalH), C_BG);

        // 3. Заголовок
        ctx.fill((int)x, (int)y, (int)(x + W), (int)(y + HEADER_H), C_HEADER);
        // Красивая линия АКЦЕНТА сверху панели
        ctx.fill((int)x, (int)y, (int)(x + W), (int)y + 2, C_ACCENT_ON);

        // Текст заголовка
        FontManager.REGULAR.drawStringWithShadow(ctx, cat.getDisplayName().toUpperCase(),
                x + 8, y + (HEADER_H - FontManager.REGULAR.getHeight()) / 2f, C_TEXT_ON);

        // Иконка развертывания
        String arrow = collapsed ? "+" : "-";
        FontManager.REGULAR.drawString(ctx, arrow,
                x + W - FontManager.REGULAR.getStringWidth(arrow) - 8,
                y + (HEADER_H - FontManager.REGULAR.getHeight()) / 2f, 0xFF666677);

        if (collapsed) return;

        float cy = y + HEADER_H;
        for (int i = 0; i < vis.size(); i++) {
            Module m = vis.get(i);
            boolean hov = inBox(mx, my, x, cy, W - DOTS_W, MODULE_H);
            boolean dHov = inBox(mx, my, x + W - DOTS_W, cy, DOTS_W, MODULE_H);

            // Подсветка при наведении
            if (hov || dHov) {
                ctx.fill((int)x, (int)cy, (int)(x + W), (int)(cy + MODULE_H), C_HOVER);
            }

            // Индикатор включенного модуля (тонкая линия слева)
            if (m.isEnabled()) {
                ctx.fill((int)x, (int)cy, (int)x + 2, (int)(cy + MODULE_H), C_ACCENT_ON);
            }

            // Текст (если биндится - желтый, если вкл - белый, выкл - серый)
            String displayName = (bindingModule == m) ? "[ Press Key... ]" : m.getName();
            int col = (bindingModule == m) ? 0xFFFFD700 : (m.isEnabled() ? C_TEXT_ON : C_TEXT_OFF);

            // Отодвинул текст от края на 8 пикселей
            FontManager.REGULAR.drawString(ctx, displayName, x + 8,
                    cy + (MODULE_H - FontManager.REGULAR.getHeight()) / 2f, col);


            // =========================================================
            // ВОТ ЗДЕСЬ Я ИЗМЕНИЛ КОД ОТРИСОВКИ ШЕСТЕРЕНКИ
            // =========================================================
            int iconCol = (openSettings == i) ? C_ACCENT_ON : (dHov ? C_TEXT_ON : 0xFF555555);
            String gearIcon = "\uf013"; // Код шестеренки в FontAwesome

            // Обязательно используем FontManager.ICONS, а не REGULAR
            FontManager.ICONS.drawString(ctx, gearIcon,
                    x + W - DOTS_W + (DOTS_W - FontManager.ICONS.getStringWidth(gearIcon)) / 2f,
                    cy + (MODULE_H - FontManager.ICONS.getHeight()) / 2f, iconCol);
            // =========================================================


            cy += MODULE_H;

            // Рендер настроек (если открыты)
            if (openSettings == i) {
                cy = renderSettings(ctx, mx, my, m, i, cy);
            }
        }
    }

    private float renderSettings(DrawContext ctx, int mx, int my, Module m, int modIdx, float cy) {
        List<Setting<?>> settings = m.getSettings();

        // Фон для всего блока настроек
        float settingsH = settings.size() * SETTING_H;
        ctx.fill((int)x, (int)cy, (int)(x + W), (int)(cy + settingsH), C_SETTING_BG);
        // Тонкая линия слева, чтобы отделить настройки визуально
        ctx.fill((int)x, (int)cy, (int)x + 1, (int)(cy + settingsH), 0xFF333344);

        for (int si = 0; si < settings.size(); si++) {
            Setting<?> s = settings.get(si);

            // Название настройки (мельче)
            FontManager.SMALL.drawString(ctx, s.getName(), x + 8,
                    cy + (SETTING_H - FontManager.SMALL.getHeight()) / 2f, C_TEXT_OFF);

            Object val = s.getValue();

            if (val instanceof Boolean b) {
                // ДИЗАЙН: Настоящий чекбокс (квадратик) вместо текста ON/OFF
                float boxSize = 10;
                float bx = x + W - boxSize - 8;
                float bby = cy + (SETTING_H - boxSize) / 2f;

                // Рамка чекбокса
                ctx.fill((int)bx - 1, (int)bby - 1, (int)(bx + boxSize + 1), (int)(bby + boxSize + 1), 0xFF444455);
                // Фон (черный)
                ctx.fill((int)bx, (int)bby, (int)(bx + boxSize), (int)(bby + boxSize), C_SETTING_BG);

                // Заполняем цветом, если включено
                if (b) {
                    ctx.fill((int)bx + 2, (int)bby + 2, (int)(bx + boxSize - 2), (int)(bby + boxSize - 2), C_ACCENT_ON);
                }

            } else if (val instanceof Number n) {
                // ДИЗАЙН: Тонкий и стильный слайдер
                double dv  = n.doubleValue();
                double min = s.getMin() == Double.MIN_VALUE ? 0 : s.getMin();
                double max = s.getMax() == Double.MAX_VALUE ? 100 : s.getMax();

                String txt = formatNum(dv);
                FontManager.SMALL.drawString(ctx, txt,
                        x + W - FontManager.SMALL.getStringWidth(txt) - 8,
                        cy + 2, C_TEXT_ON);

                float sliderX = x + 8;
                float sliderW = W - 16;
                float sliderY = cy + SETTING_H - 4; // Линия в самом низу настройки
                float prog    = (float)((dv - min) / (max - min));

                // Серая дорожка
                ctx.fill((int)sliderX, (int)sliderY, (int)(sliderX + sliderW), (int)sliderY + 1, 0xFF333344);
                // Заполненная цветная часть
                ctx.fill((int)sliderX, (int)sliderY, (int)(sliderX + sliderW * prog), (int)sliderY + 1, C_ACCENT_ON);

                if (sliderActive && sliderModule == modIdx && sliderSetting == si) {
                    double newProg = Math.max(0, Math.min(1, (mx - sliderX) / sliderW));
                    setSettingValue(s, min + newProg * (max - min));
                }
            } else {
                // Режимы (Mode) - просто акцентный текст
                String strVal = val.toString();
                FontManager.SMALL.drawString(ctx, strVal,
                        x + W - FontManager.SMALL.getStringWidth(strVal) - 8,
                        cy + (SETTING_H - FontManager.SMALL.getHeight()) / 2f, C_ACCENT_ON);
            }
            cy += SETTING_H;
        }
        return cy;
    }

    // ================================= ОРИГИНАЛЬНАЯ ЛОГИКА (Не трогал) ================================= //

    public boolean mouseClicked(double mx, double my, int btn) {
        if (inBox(mx, my, x, y, W, HEADER_H)) {
            if (btn == 0) { dragging = true; dox = (float)(mx-x); doy = (float)(my-y); }
            else if (btn == 1) { collapsed = !collapsed; openSettings = -1; }
            return true;
        }
        if (collapsed) return false;

        float cy = y + HEADER_H;
        List<Module> vis = filtered("");
        for (int i = 0; i < vis.size(); i++) {
            Module m = vis.get(i);

            if (inBox(mx, my, x, cy, W - DOTS_W, MODULE_H)) {
                if (btn == 0) {
                    m.toggle();
                    return true;
                } else if (btn == 2) {
                    bindingModule = m;
                    return true;
                }
            }

            if (inBox(mx, my, x + W - DOTS_W, cy, DOTS_W, MODULE_H) && btn == 0) {
                openSettings = (openSettings == i) ? -1 : i; return true;
            }
            cy += MODULE_H;

            if (openSettings == i) {
                List<Setting<?>> settings = m.getSettings();
                for (int si = 0; si < settings.size(); si++) {
                    Setting<?> s = settings.get(si);
                    if (inBox(mx, my, x, cy, W, SETTING_H)) {
                        Object val = s.getValue();
                        if (val instanceof Boolean b) {
                            setSettingValue(s, !b);
                        } else if (val instanceof Number) {
                            double min = s.getMin() == Double.MIN_VALUE ? 0 : s.getMin();
                            double max = s.getMax() == Double.MAX_VALUE ? 100 : s.getMax();
                            float sliderX = x + 10;
                            float sliderW = W - 20;
                            double prog   = Math.max(0, Math.min(1, (mx - sliderX) / sliderW));
                            setSettingValue(s, min + prog * (max - min));
                            sliderModule  = i;
                            sliderSetting = si;
                            sliderActive  = true;
                        } else if (val instanceof String str) {
                            cycleString(s, str);
                        }
                        return true;
                    }
                    cy += SETTING_H;
                }
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                bindingModule.setKeybind(-1);
            } else {
                bindingModule.setKeybind(keyCode);
            }
            bindingModule = null;
            return true;
        }
        return false;
    }

    public void mouseDragged(double mx, double my) {
        if (dragging) { x = (float)(mx - dox); y = (float)(my - doy); }
    }

    public void mouseReleased() {
        dragging     = false;
        sliderActive = false;
    }

    public boolean mouseScrolled(double mx, double my, double delta) { return false; }

    @SuppressWarnings("unchecked")
    private <T> void setSettingValue(Setting<T> s, Object newVal) {
        try {
            if (s.getValue() instanceof Double)  s.setValue((T)(Double) newVal);
            else if (s.getValue() instanceof Float)   s.setValue((T)(Float) ((Double)newVal).floatValue());
            else if (s.getValue() instanceof Integer) s.setValue((T)(Integer)((Double)newVal).intValue());
            else if (s.getValue() instanceof Boolean) s.setValue((T)(Boolean) newVal);
            else if (s.getValue() instanceof String)  s.setValue((T)(String)  newVal);
        } catch (Exception ignored) {}
    }

    private void cycleString(Setting<?> s, String current) {
        String desc = s.getDescription();
        String[] parts = desc.split("[/|,]");
        if (parts.length < 2) return;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].trim().equalsIgnoreCase(current)) {
                setSettingValue(s, parts[(i + 1) % parts.length].trim());
                return;
            }
        }
        setSettingValue(s, parts[0].trim());
    }

    private String formatNum(double v) {
        return v == Math.floor(v) ? String.valueOf((int)v) : String.format("%.2f", v);
    }

    private List<Module> filtered(String f) {
        if (f == null || f.isEmpty()) return modules;
        return modules.stream()
                .filter(m -> m.getName().toLowerCase().contains(f.toLowerCase()))
                .toList();
    }

    private boolean inBox(double mx, double my, float bx, float by, float bw, float bh) {
        return mx >= bx && mx <= bx+bw && my >= by && my <= by+bh;
    }

    public float getX() { return x; }
    public float getY() { return y; }
}