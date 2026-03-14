package com.pulse.client.gui;

import com.pulse.client.PulseClient;
import com.pulse.client.gui.font.FontManager;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.render.*;
import com.pulse.client.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGUI extends Screen {

    private static final int SIDEBAR_W  = 150;
    private static final int HEADER_H   = 52;
    private static final int CAT_H      = 36;
    private static final int CAT_GAP    = 3;
    private static final int CARD_H     = 52;
    private static final int CARD_GAP   = 10;
    private static final int PAD        = 14;
    private static final int SET_ROW_H  = 22;
    private static final int SEARCH_H   = 30;

    private static final int BG          = 0xFF0D0D11;
    private static final int ACCENT      = 0xFFC62828;
    private static final int CARD_OFF    = 0xFF161619;
    private static final int CARD_BORDER = 0xFF28282E;
    private static final int TXT         = 0xFFFFFFFF;
    private static final int TXT_DIM     = 0xFFA0A0B0;
    private static final int TXT_DARK    = 0xFF8B8B9E;
    private static final int SEARCH_BG   = 0xFF151518;
    private static final int DIVIDER     = 0xFF1E1E24;

    private float winX, winY, winW, winH;
    private boolean dragging;
    private float dragOX, dragOY;

    private Category selectedCategory = Category.COMBAT;
    private String searchText = "";
    private boolean searchFocused;
    private long cursorBlink;
    private float scrollOff, scrollTarget, scrollMax;

    private float openAnim;
    private boolean closingFlag;
    private final Map<Category, Float> catAnim  = new HashMap<>();
    private final Map<Module, Float>   togAnim  = new HashMap<>();
    private final Map<Module, Float>   expAnim  = new HashMap<>();
    private final Map<Module, Boolean> expanded = new HashMap<>();

    private Module bindingModule;
    private Module sliderMod;
    private int    sliderIdx = -1;
    private boolean sliderActive;

    public ClickGUI() {
        super(Text.literal("PulseClient"));
    }

    @Override
    protected void init() {
        winW = Math.min(720, width - 40);
        winH = Math.min(460, height - 40);
        winX = (width  - winW) / 2f;
        winY = (height - winH) / 2f;
        openAnim    = 0f;
        closingFlag = false;
        scrollOff = scrollTarget = scrollMax = 0;

        for (Category c : Category.values())
            catAnim.putIfAbsent(c, c == selectedCategory ? 1f : 0f);
        for (Module m : allModules()) {
            togAnim.putIfAbsent(m, m.isEnabled() ? 1f : 0f);
            expAnim.putIfAbsent(m, 0f);
            expanded.putIfAbsent(m, false);
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // убираем dirt-фон
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float target = closingFlag ? 0f : 1f;
        openAnim = AnimationUtil.animate(openAnim, target, 0.35f, delta);
        if (closingFlag && openAnim < 0.01f) {
            closingFlag = false;
            if (client != null) client.setScreen(null);
            return;
        }

        float a = AnimationUtil.easeOutCubic(openAnim);
        if (a < 0.005f) return;

        // Лёгкое затемнение вместо drawBlurBackground (который давал слишком тёмный фон)
        ctx.fill(0, 0, width, height, (int)(80 * a) << 24);

        tickAnimations(mx, my, delta);

        float scale = 0.85f + 0.15f * a;
        float cx    = winX + winW / 2f;
        float cy    = winY + winH / 2f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, cy, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.getMatrices().translate(-cx, -cy, 0);

        SmoothRenderer.drawSmoothRect(ctx, winX - 1, winY - 1, winW + 2, winH + 2, 12,
                ColorUtil.withAlpha(DIVIDER, (int)(255 * a)));
        SmoothRenderer.drawSmoothRect(ctx, winX, winY, winW, winH, 12,
                ColorUtil.withAlpha(BG, (int)(255 * a)));

        Render2DEngine.drawVLine(ctx, winX + SIDEBAR_W, winY + 8, winH - 16, 1,
                ColorUtil.withAlpha(DIVIDER, (int)(200 * a)));

        drawSidebar(ctx, mx, my, winY, a);
        drawSearch(ctx, mx, my, winY, a);
        drawModules(ctx, mx, my, winY, a, delta);

        ctx.getMatrices().pop();
        super.render(ctx, mx, my, delta);
    }
    // ── tickAnimations ────────────────────────────────────────────
    private void tickAnimations(int mx, int my, float delta) {
        for (Category c : Category.values()) {
            float t = c == selectedCategory ? 1f : 0f;
            catAnim.put(c, AnimationUtil.animate(catAnim.get(c), t, 0.35f, delta));
        }
        for (Module m : allModules()) {
            togAnim.put(m, AnimationUtil.animate(togAnim.get(m), m.isEnabled() ? 1f : 0f, 0.3f, delta));
            boolean exp = expanded.getOrDefault(m, false);
            expAnim.put(m, AnimationUtil.animate(expAnim.get(m), exp ? 1f : 0f, 0.3f, delta));
        }
        scrollOff = AnimationUtil.lerp(scrollOff, scrollTarget, 0.35f);
    }

    // ── Sidebar ───────────────────────────────────────────────────
    private float sidebarCatStartY(float wy) {
        float logoY = wy + 20;
        float lineY = logoY + FontManager.TITLE.getHeight() + 6;
        float labY  = lineY + 14;
        return labY + FontManager.SMALL.getHeight() + 8;
    }

    private void drawSidebar(DrawContext ctx, int mx, int my, float wy, float alpha) {
        float sx    = winX;
        float logoY = wy + 20;
        float pW    = FontManager.TITLE.getStringWidth("P");
        FontManager.TITLE.drawStringWithShadow(ctx, "P",     sx + 18,        logoY, ColorUtil.withAlpha(ACCENT, (int)(255 * alpha)));
        FontManager.TITLE.drawStringWithShadow(ctx, "ulse",  sx + 18 + pW + 1, logoY, ColorUtil.withAlpha(TXT,   (int)(255 * alpha)));

        float lineY = logoY + FontManager.TITLE.getHeight() + 6;
        Render2DEngine.drawHLine(ctx, sx + 18, lineY, SIDEBAR_W - 36, 1, ColorUtil.withAlpha(DIVIDER, (int)(200 * alpha)));

        float labY = lineY + 14;
        FontManager.SMALL.drawString(ctx, "Основные", sx + 20, labY, ColorUtil.withAlpha(TXT_DARK, (int)(255 * alpha)));

        float cy = sidebarCatStartY(wy);
        for (Category c : Category.values()) {
            float ca  = catAnim.getOrDefault(c, 0f);
            boolean hov = mx >= sx && mx <= sx + SIDEBAR_W && my >= cy && my <= cy + CAT_H;
            float   bg  = Math.max(ca, hov && ca < 0.3f ? 0.15f : 0f);

            if (bg > 0.01f)
                SmoothRenderer.drawSmoothRect(ctx, sx + 10, cy, SIDEBAR_W - 20, CAT_H, 8,
                        ColorUtil.withAlpha(ACCENT, (int)(bg * 220 * alpha)));

            if (ca > 0.3f)
                GlowRenderer.drawRectGlow(ctx, sx + 10, cy, SIDEBAR_W - 20, CAT_H, 8,
                        ACCENT, ca * 0.5f * alpha, 3);

            int txtCol = ColorUtil.withAlpha(
                    ColorUtil.lerp(TXT_DIM, TXT, Math.max(ca, hov ? 0.5f : 0f)),
                    (int)(255 * alpha));
            FontManager.ICONS.drawString(ctx, catIcon(c),
                    sx + 22, cy + (CAT_H - FontManager.ICONS.getHeight()) / 2f, txtCol);
            FontManager.REGULAR.drawString(ctx, c.getDisplayName(),
                    sx + 44, cy + (CAT_H - FontManager.REGULAR.getHeight()) / 2f, txtCol);

            cy += CAT_H + CAT_GAP;
        }
    }

    // ── Search ────────────────────────────────────────────────────
    private float searchX() { return winX + SIDEBAR_W + PAD; }
    private float searchW() { return winW - SIDEBAR_W - PAD * 2; }

    private void drawSearch(DrawContext ctx, int mx, int my, float wy, float alpha) {
        float sx = searchX();
        float sy = wy + (HEADER_H - SEARCH_H) / 2f;
        float sw = searchW();

        int outline = searchFocused ? ACCENT : 0xFF222228;
        SmoothRenderer.drawSmoothRect(ctx, sx - 1, sy - 1, sw + 2, SEARCH_H + 2, 8,
                ColorUtil.withAlpha(outline, (int)(255 * alpha)));
        SmoothRenderer.drawSmoothRect(ctx, sx, sy, sw, SEARCH_H, 8,
                ColorUtil.withAlpha(searchFocused ? 0xFF1A1A20 : SEARCH_BG, (int)(255 * alpha)));

        FontManager.ICONS.drawString(ctx, "\uf002",
                sx + 10, sy + (SEARCH_H - FontManager.ICONS.getHeight()) / 2f,
                ColorUtil.withAlpha(TXT_DARK, (int)(255 * alpha)));

        String text = searchText;
        if (searchText.isEmpty() && !searchFocused)
            text = "Поиск";
        else if (searchFocused && (System.currentTimeMillis() - cursorBlink) / 500 % 2 == 0)
            text = searchText + "|";

        int tc = (searchText.isEmpty() && !searchFocused) ? TXT_DARK : TXT;
        FontManager.REGULAR.drawString(ctx, text,
                sx + 30, sy + (SEARCH_H - FontManager.REGULAR.getHeight()) / 2f,
                ColorUtil.withAlpha(tc, (int)(255 * alpha)));
    }

    // ── Modules ───────────────────────────────────────────────────
    private void drawModules(DrawContext ctx, int mx, int my, float wy, float alpha, float delta) {
        float ax = winX + SIDEBAR_W;
        float ay = wy + HEADER_H;
        float aw = winW - SIDEBAR_W;
        float ah = winH - HEADER_H;

        ScissorUtil.push(ax, ay, aw, ah);

        List<Module> mods = filteredModules();
        float cw = (aw - PAD * 3) / 2f;
        float lx = ax + PAD;
        float rx = ax + PAD * 2 + cw;
        float y  = ay + PAD - scrollOff;

        for (int i = 0; i < mods.size(); i += 2) {
            Module left  = mods.get(i);
            Module right = (i + 1 < mods.size()) ? mods.get(i + 1) : null;
            float  lh    = cardHeight(left);
            float  rh    = right != null ? cardHeight(right) : 0;
            float  rowH  = Math.max(lh, rh);

            boolean vis = (y + rowH > ay - 20) && (y < ay + ah + 20);
            if (vis) {
                drawCard(ctx, left,  lx, y, cw, lh, mx, my, alpha);
                if (right != null) drawCard(ctx, right, rx, y, cw, rh, mx, my, alpha);
            }
            y += rowH + CARD_GAP;
        }

        ScissorUtil.pop();
        float contentH = y + scrollOff - ay - PAD;
        scrollMax   = Math.max(0, contentH - ah + PAD);
        scrollTarget = clamp(scrollTarget, 0, scrollMax);
    }

    private void drawCard(DrawContext ctx, Module m, float x, float y, float w, float h,
                          int mx, int my, float alpha) {
        float tog = togAnim.getOrDefault(m, 0f);
        float exp = expAnim.getOrDefault(m, 0f);

        ScissorUtil.push(x - 2, y - 2, w + 4, h + 4);

        if (tog > 0.05f)
            GlowRenderer.drawRectGlow(ctx, x, y, w, h, 8, ACCENT, tog * 0.7f, 3);

        int bg = ColorUtil.lerp(CARD_OFF, ACCENT, tog);
        if (tog < 0.9f) {
            int ba = (int)(255 * (1f - tog) * alpha);
            SmoothRenderer.drawSmoothRect(ctx, x - 1, y - 1, w + 2, h + 2, 8,
                    ColorUtil.withAlpha(CARD_BORDER, ba));
        }
        SmoothRenderer.drawSmoothRect(ctx, x, y, w, h, 8,
                ColorUtil.withAlpha(bg, (int)(255 * alpha)));

        String name = (bindingModule == m) ? "Нажмите клавишу..." : m.getName();
        FontManager.REGULAR.drawStringWithShadow(ctx, name,
                x + 12, y + 10, ColorUtil.withAlpha(TXT, (int)(255 * alpha)));

        int descCol = ColorUtil.withAlpha(
                ColorUtil.lerp(TXT_DARK, 0xCCFFFFFF, tog), (int)(255 * alpha));
        FontManager.SMALL.drawString(ctx, m.getDescription(),
                x + 12, y + 10 + FontManager.REGULAR.getHeight() + 3, descCol);

        if (!m.getSettings().isEmpty()) {
            boolean isExp = expanded.getOrDefault(m, false);
            int gc = ColorUtil.withAlpha(
                    isExp ? TXT : (tog > 0.5f ? 0xDDFFFFFF : TXT_DARK),
                    (int)(255 * alpha));
            FontManager.ICONS.drawString(ctx, "\uf013", x + w - 28, y + 12, gc);
        }

        if (exp > 0.01f)
            drawSettings(ctx, m, x, y + CARD_H, w, exp, tog, mx, my, alpha);

        ScissorUtil.pop();
    }

    private void drawSettings(DrawContext ctx, Module m, float x, float y, float w,
                              float exp, float tog, int mx, int my, float alpha) {
        List<Setting<?>> sets = m.getSettings();
        if (sets.isEmpty()) return;

        float a = alpha * exp;
        Render2DEngine.drawHLine(ctx, x + 12, y, w - 24, 1,
                ColorUtil.withAlpha(0xFFFFFFFF, (int)(25 * a)));

        float sy = y + 5;
        for (int si = 0; si < sets.size(); si++) {
            Setting<?> s   = sets.get(si);
            Object     val = s.getValue();
            int tc  = ColorUtil.withAlpha(ColorUtil.lerp(TXT_DIM, TXT, tog), (int)(255 * a));
            int vc  = ColorUtil.withAlpha(TXT, (int)(255 * a));

            FontManager.SMALL.drawString(ctx, s.getName(), x + 12, sy + 4, tc);

            if (val instanceof Boolean b) {
                int onCol  = ColorUtil.withAlpha(ACCENT,      (int)(255 * a));
                int offCol = ColorUtil.withAlpha(0xFF444455,  (int)(255 * a));
                SmoothRenderer.drawSmoothRect(ctx, x + w - 20, sy + 6, 8, 8, 4, b ? onCol : offCol);

            } else if (val instanceof Number n) {
                double min  = s.getMin() == Double.MIN_VALUE ? 0   : s.getMin();
                double max  = s.getMax() == Double.MAX_VALUE ? 100 : s.getMax();
                float  prog = (float) ((n.doubleValue() - min) / (max - min));

                String txt = fmtNum(n.doubleValue());
                FontManager.SMALL.drawString(ctx, txt,
                        x + w - FontManager.SMALL.getStringWidth(txt) - 12, sy + 4, vc);

                float bx = x + 12, bw = w - 24, by = sy + 15;
                float fillWidth = Math.max(0, bw * prog);

                int fill  = tog > 0.5f
                        ? ColorUtil.withAlpha(TXT,        (int)(200 * a))
                        : ColorUtil.withAlpha(ACCENT,     (int)(220 * a));
                int track = ColorUtil.withAlpha(0x40FFFFFF, (int)(255 * a));

                SmoothRenderer.drawSmoothRect(ctx, bx, by, bw, 4, 2, track);
                if (fillWidth > 0)
                    SmoothRenderer.drawSmoothRect(ctx, bx, by, fillWidth, 4, 2, fill);

                float knobX = clamp(bx + fillWidth - 3, bx - 1, bx + bw - 5);
                SmoothRenderer.drawSmoothRect(ctx, knobX, by - 1, 6, 6, 3,
                        ColorUtil.withAlpha(TXT, (int)(255 * a)));

                if (sliderActive && sliderMod == m && sliderIdx == si) {
                    double np = clamp((mx - bx) / bw, 0, 1);
                    setSetting(s, min + np * (max - min));
                }
            } else {
                String sv = val.toString();
                FontManager.SMALL.drawString(ctx, sv,
                        x + w - FontManager.SMALL.getStringWidth(sv) - 12, sy + 4, vc);
            }
            sy += SET_ROW_H;
        }
    }

    // ── Transform helpers ─────────────────────────────────────────
    private double transformMouseX(double mx) {
        float a     = AnimationUtil.easeOutCubic(openAnim);
        float scale = 0.85f + 0.15f * a;
        float cx    = winX + winW / 2f;
        return cx + (mx - cx) / scale;
    }

    private double transformMouseY(double my) {
        float a     = AnimationUtil.easeOutCubic(openAnim);
        float scale = 0.85f + 0.15f * a;
        float cy    = winY + winH / 2f;
        return cy + (my - cy) / scale;
    }

    // ── Input ─────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double rawMx, double rawMy, int btn) {
        if (closingFlag) return false;
        double mx = transformMouseX(rawMx);
        double my = transformMouseY(rawMy);

        float sx = searchX();
        float sy = winY + (HEADER_H - SEARCH_H) / 2f;
        if (hit(mx, my, sx, sy, searchW(), SEARCH_H)) {
            searchFocused = true;
            cursorBlink   = System.currentTimeMillis();
            return true;
        }
        searchFocused = false;

        float cy = sidebarCatStartY(winY);
        for (Category c : Category.values()) {
            if (hit(mx, my, winX + 10, cy, SIDEBAR_W - 20, CAT_H)) {
                selectedCategory = c;
                scrollTarget = scrollOff = 0;
                return true;
            }
            cy += CAT_H + CAT_GAP;
        }

        if (hit(mx, my, winX, winY, winW, HEADER_H)) {
            dragging = true;
            dragOX   = (float) rawMx - winX;
            dragOY   = (float) rawMy - winY;
            return true;
        }

        float ax = winX + SIDEBAR_W, ay = winY + HEADER_H;
        float aw = winW - SIDEBAR_W, ah = winH - HEADER_H;
        if (!hit(mx, my, ax, ay, aw, ah)) return super.mouseClicked(rawMx, rawMy, btn);

        List<Module> mods = filteredModules();
        float cw = (aw - PAD * 3) / 2f;
        float lx = ax + PAD;
        float rx = ax + PAD * 2 + cw;
        float y  = ay + PAD - scrollOff;

        for (int i = 0; i < mods.size(); i += 2) {
            Module left  = mods.get(i);
            Module right = (i + 1 < mods.size()) ? mods.get(i + 1) : null;
            float  rowH  = Math.max(cardHeight(left), right != null ? cardHeight(right) : 0);

            if (clickCard(left, lx, y, cw, cardHeight(left), mx, my, btn)) return true;
            if (right != null && clickCard(right, rx, y, cw, cardHeight(right), mx, my, btn)) return true;

            y += rowH + CARD_GAP;
        }
        return super.mouseClicked(rawMx, rawMy, btn);
    }

    private boolean clickCard(Module m, float x, float y, float w, float h,
                              double mx, double my, int btn) {
        if (!hit(mx, my, x, y, w, h)) return false;

        if (my <= y + CARD_H) {
            if (!m.getSettings().isEmpty() && mx >= x + w - 35) {
                if (btn == 0 || btn == 1) {
                    expanded.put(m, !expanded.getOrDefault(m, false));
                    return true;
                }
            }
            if (btn == 0) { m.toggle(); return true; }
            if (btn == 1 && !m.getSettings().isEmpty()) {
                expanded.put(m, !expanded.getOrDefault(m, false));
                return true;
            }
            if (btn == 2) { bindingModule = m; return true; }
            return true;
        }

        if (expAnim.getOrDefault(m, 0f) < 0.3f) return true;

        List<Setting<?>> sets = m.getSettings();
        float sy = y + CARD_H + 5;
        for (int si = 0; si < sets.size(); si++) {
            if (my >= sy && my < sy + SET_ROW_H) {
                Setting<?> s   = sets.get(si);
                Object     val = s.getValue();
                if (val instanceof Boolean b) {
                    setSetting(s, !b);
                } else if (val instanceof Number) {
                    sliderMod = m; sliderIdx = si; sliderActive = true;
                    double min = s.getMin() == Double.MIN_VALUE ? 0   : s.getMin();
                    double max = s.getMax() == Double.MAX_VALUE ? 100 : s.getMax();
                    float bx = x + 12, bw = w - 24;
                    double np = clamp((mx - bx) / bw, 0, 1);
                    setSetting(s, min + np * (max - min));
                } else if (val instanceof String str) {
                    cycleStr(s, str);
                }
                return true;
            }
            sy += SET_ROW_H;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging) {
            winX = (float) mx - dragOX;
            winY = (float) my - dragOY;
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        dragging     = false;
        sliderActive = false;
        sliderMod    = null;
        return super.mouseReleased(mx, my, btn);
    }

    // ── 1.20.4: один параметр amount вместо (h, v) ───────────────
    @Override
    public boolean mouseScrolled(double rawMx, double rawMy, double horizontal, double vertical) {
        double mx = transformMouseX(rawMx);
        double my = transformMouseY(rawMy);
        float ax = winX + SIDEBAR_W;
        float ay = winY + HEADER_H;
        if (hit(mx, my, ax, ay, winW - SIDEBAR_W, winH - HEADER_H)) {
            scrollTarget = clamp(scrollTarget - (float) vertical * 28, 0, scrollMax);
            return true;
        }
        return super.mouseScrolled(rawMx, rawMy, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (bindingModule != null) {
            bindingModule.setKeybind(
                    (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE) ? -1 : key);
            bindingModule = null;
            return true;
        }
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) { searchText = ""; searchFocused = false; return true; }
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (searchFocused && chr >= 32 && searchText.length() < 30) {
            searchText += chr;
            return true;
        }
        return super.charTyped(chr, mods);
    }

    @Override
    public void close() { if (!closingFlag) closingFlag = true; }

    @Override public boolean shouldPause()        { return false; }
    @Override public boolean shouldCloseOnEsc()   { return true;  }

    // ── Helpers ───────────────────────────────────────────────────
    private List<Module> allModules() {
        return PulseClient.getInstance().getModuleManager().getModules();
    }

    private List<Module> filteredModules() {
        List<Module> all = PulseClient.getInstance()
                .getModuleManager().getModulesByCategory(selectedCategory);
        if (searchText.isEmpty()) return all;
        String q = searchText.toLowerCase();
        return all.stream().filter(m -> m.getName().toLowerCase().contains(q)).toList();
    }

    private float cardHeight(Module m) {
        float e = expAnim.getOrDefault(m, 0f);
        if (e < 0.01f || m.getSettings().isEmpty()) return CARD_H;
        return CARD_H + (m.getSettings().size() * SET_ROW_H + 10) * e;
    }

    private String catIcon(Category c) {
        return switch (c) {
            case COMBAT   -> "\uf05b";
            case MOVEMENT -> "\uf11b";
            case RENDER   -> "\uf06e";
            case PLAYER   -> "\uf007";
        };
    }

    private boolean hit(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float  clamp(float  v, float  min, float  max) { return Math.max(min, Math.min(max, v)); }
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }

    @SuppressWarnings("unchecked")
    private <T> void setSetting(Setting<T> s, Object v) {
        try {
            if      (s.getValue() instanceof Double)  s.setValue((T)(Double) v);
            else if (s.getValue() instanceof Float)   s.setValue((T)(Float)((Double) v).floatValue());
            else if (s.getValue() instanceof Integer) s.setValue((T)(Integer)((Double) v).intValue());
            else if (s.getValue() instanceof Boolean) s.setValue((T)(Boolean) v);
            else if (s.getValue() instanceof String)  s.setValue((T)(String) v);
        } catch (Exception ignored) {}
    }

    private void cycleStr(Setting<?> s, String cur) {
        String[] p = s.getDescription().split("[/|,]");
        if (p.length < 2) return;
        for (int i = 0; i < p.length; i++) {
            if (p[i].trim().equalsIgnoreCase(cur)) {
                setSetting(s, p[(i + 1) % p.length].trim());
                return;
            }
        }
        setSetting(s, p[0].trim());
    }

    private String fmtNum(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format("%.1f", v);
    }

    // ── SmoothRenderer (без изменений — ctx.fill() одинаков) ─────
    public static class SmoothRenderer {
        public static void drawSmoothRect(DrawContext ctx, float x, float y, float w, float h,
                                          float r, int color) {
            int ix = Math.round(x), iy = Math.round(y);
            int iw = Math.round(w), ih = Math.round(h);
            int ir = (int) Math.min(Math.max(r, 0), Math.min(iw, ih) / 2f);

            int baseAlpha = (color >>> 24) & 0xFF;
            if (baseAlpha == 0 || iw <= 0 || ih <= 0) return;

            if (ir <= 0) { ctx.fill(ix, iy, ix + iw, iy + ih, color); return; }

            int rgb = color & 0x00FFFFFF;

            ctx.fill(ix + ir, iy,      ix + iw - ir, iy + ih,      color);
            ctx.fill(ix,      iy + ir, ix + ir,      iy + ih - ir, color);
            ctx.fill(ix + iw - ir, iy + ir, ix + iw, iy + ih - ir, color);

            for (int dy = 0; dy < ir; dy++) {
                for (int dx = 0; dx < ir; dx++) {
                    float fcx  = ir - 0.5f - dx;
                    float fcy  = ir - 0.5f - dy;
                    float dist = (float) Math.sqrt(fcx * fcx + fcy * fcy);
                    float alphaMult = ir - dist + 0.5f;

                    if      (alphaMult >= 1f) alphaMult = 1f;
                    else if (alphaMult <= 0f) continue;

                    int finalAlpha = (int)(baseAlpha * alphaMult);
                    if (finalAlpha <= 0) continue;
                    int px = (finalAlpha << 24) | rgb;

                    ctx.fill(ix + dx,           iy + dy,           ix + dx + 1,       iy + dy + 1,       px);
                    ctx.fill(ix + iw - 1 - dx,  iy + dy,           ix + iw - dx,      iy + dy + 1,       px);
                    ctx.fill(ix + dx,            iy + ih - 1 - dy, ix + dx + 1,        iy + ih - dy,      px);
                    ctx.fill(ix + iw - 1 - dx,  iy + ih - 1 - dy, ix + iw - dx,       iy + ih - dy,      px);
                }
            }
        }
    }
}