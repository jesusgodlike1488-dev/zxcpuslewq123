package com.pulse.client.gui;

import com.pulse.client.PulseClient;
import com.pulse.client.gui.font.FontManager;
import com.pulse.client.util.BlurUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

public class MainMenuScreen extends Screen {

    // ── star animation ─────────────────────────────────────────────────── //
    private final float[] starX, starY, starAlpha, starSpeed;
    private static final int STAR_COUNT = 60;

    // ── button definitions ─────────────────────────────────────────────── //
    private record Btn(String label, String sub, int accent) {}
    private static final Btn[] BUTTONS = {
            new Btn("Одиночная игра",  "Singleplayer",   0xFF1E90FF),
            new Btn("Мультиплеер",     "Multiplayer",    0xFF1E90FF),
            new Btn("Аккаунты",        "Account Manager",0xFF7B2FBE),
            new Btn("Настройки",       "Options",        0xFF2A6A2A),
            new Btn("Выход",           "Quit",           0xFF8B2222),
    };

    private int     hoveredBtn = -1;
    private float[] btnAnim    = new float[BUTTONS.length];

    public MainMenuScreen() {
        super(Text.literal("PulseClient"));
        FontManager.init();

        starX     = new float[STAR_COUNT];
        starY     = new float[STAR_COUNT];
        starAlpha = new float[STAR_COUNT];
        starSpeed = new float[STAR_COUNT];

        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i]     = (float) Math.random();
            starY[i]     = (float) Math.random();
            starAlpha[i] = (float) Math.random();
            starSpeed[i] = 0.003f + (float)(Math.random() * 0.005);
        }
    }

    // ── убираем dirt-фон ─────────────────────────────────────────────────//
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // намеренно пусто — наш фон рисуется в render()
    }

    // ─────────────────────────────── render ─────────────────────────────── //
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Background
        ctx.fill(0, 0, width, height, 0xFF050510);
        BlurUtil.drawGradientRect(ctx, 0, 0, width, height / 2, 0xFF0A0A25, 0xFF050510);
        BlurUtil.drawGradientRect(ctx, 0, height / 2, width, height, 0xFF050510, 0xFF020208);

        renderStars(ctx, delta);
        renderBranding(ctx);
        renderButtons(ctx, mx, my, delta);
        renderFooter(ctx);

        super.render(ctx, mx, my, delta);
    }

    private void renderStars(DrawContext ctx, float delta) {
        for (int i = 0; i < STAR_COUNT; i++) {
            starAlpha[i] += starSpeed[i] * delta;
            if (starAlpha[i] > 1f) {
                starAlpha[i] = 0f;
                starX[i] = (float) Math.random();
                starY[i] = (float) Math.random();
            }
            float a   = (float) Math.sin(starAlpha[i] * Math.PI);
            int   col = ((int)(a * 120) << 24) | 0xFFFFFF;
            int   sx  = (int)(starX[i] * width);
            int   sy  = (int)(starY[i] * height);
            ctx.fill(sx, sy, sx + 1, sy + 1, col);
        }
    }

    private void renderBranding(DrawContext ctx) {
        int lx = 40;
        int ly = height / 2 - 70;

        String pulse = "Pulse";
        String cl    = "Client";
        int    pw    = FontManager.TITLE.getStringWidth(pulse);
        FontManager.TITLE.drawStringWithShadow(ctx, pulse, lx, ly, 0xFF1E90FF);
        FontManager.TITLE.drawStringWithShadow(ctx, cl, lx + pw + 3, ly, 0xFFFFFFFF);

        int lineY = ly + FontManager.TITLE.getHeight() + 3;
        ctx.fill(lx, lineY, lx + pw + 3 + FontManager.TITLE.getStringWidth(cl), lineY + 1, 0xFF1E90FF);

        int dy = lineY + 10;
        FontManager.SMALL.drawString(ctx, "Fabric клиент для Minecraft 1.20.4", lx, dy, 0xFF888888);
        dy += FontManager.SMALL.getHeight() + 5;
        FontManager.SMALL.drawString(ctx, "v" + PulseClient.VERSION + "  •  Right Shift для GUI чит-меню", lx, dy, 0xFF444444);

        if (client != null && client.getSession() != null) {
            String name = client.getSession().getUsername();
            int bx = lx, by = dy + FontManager.SMALL.getHeight() + 14;
            int bw = Math.max(160, FontManager.REGULAR.getStringWidth(name) + 40), bh = 28;
            BlurUtil.drawRoundedRect(ctx, bx, by, bw, bh, 5f, 0xFF0D1627);
            BlurUtil.drawRoundedRectOutline(ctx, bx, by, bw, bh, 5f, 1f, 0xFF1E3A6E);
            FontManager.SMALL.drawString(ctx, "Вошли как:", bx + 8, by + 3, 0xFF555555);
            FontManager.REGULAR.drawString(ctx, name, bx + 8,
                    by + 3 + FontManager.SMALL.getHeight() + 2, 0xFF1E90FF);
        }
    }

    private void renderButtons(DrawContext ctx, int mx, int my, float delta) {
        int btnW = 260, btnH = 58, btnGap = 10;
        int totalH = BUTTONS.length * (btnH + btnGap) - btnGap;
        int startX = width - btnW - 50;
        int startY = (height - totalH) / 2;

        hoveredBtn = -1;
        for (int i = 0; i < BUTTONS.length; i++) {
            int by = startY + i * (btnH + btnGap);
            boolean hov = mx >= startX && mx <= startX + btnW && my >= by && my <= by + btnH;
            if (hov) hoveredBtn = i;

            btnAnim[i] = hov
                    ? Math.min(1f, btnAnim[i] + 0.12f * delta)
                    : Math.max(0f, btnAnim[i] - 0.12f * delta);

            float t   = btnAnim[i];
            int   bx  = (int)(startX - t * 5);
            int   acc = BUTTONS[i].accent();

            BlurUtil.drawRoundedRect(ctx, bx + 2, by + 2, btnW, btnH, 5f, 0x44000000);
            BlurUtil.drawRoundedRect(ctx, bx, by, btnW, btnH, 5f, blend(0xFF0E0E18, acc, t * 0.2f));
            BlurUtil.drawRoundedRectOutline(ctx, bx, by, btnW, btnH, 5f, 1f, blend(0xFF1C1C2C, acc, t * 0.7f));

            if (t > 0.05f) {
                int aa = (int)(t * 200);
                ctx.fill(bx, by + 4, bx + 2, by + btnH - 4, (aa << 24) | (acc & 0x00FFFFFF));
            }

            int totalTextH = FontManager.REGULAR.getHeight() + 4 + FontManager.SMALL.getHeight();
            float ty = by + (btnH - totalTextH) / 2f;
            FontManager.REGULAR.drawString(ctx, BUTTONS[i].label(), bx + 14, ty, 0xFFFFFFFF);
            FontManager.SMALL.drawString(ctx, BUTTONS[i].sub(), bx + 14,
                    ty + FontManager.REGULAR.getHeight() + 4, 0xFF667799);
        }
    }

    private void renderFooter(DrawContext ctx) {
        String txt = "Fabric 1.20.4  •  PulseClient " + PulseClient.VERSION;
        FontManager.SMALL.drawCenteredString(ctx, txt, width / 2f, height - 11, 0xFF2A2A3A);
    }

    private static int blend(int base, int target, float t) {
        int ba=(base>>24)&0xFF,  br=(base>>16)&0xFF,  bg=(base>>8)&0xFF,  bb=base&0xFF;
        int ta=(target>>24)&0xFF,tr=(target>>16)&0xFF,tg=(target>>8)&0xFF,tb=target&0xFF;
        return (lerp(ba,ta,t)<<24)|(lerp(br,tr,t)<<16)|(lerp(bg,tg,t)<<8)|lerp(bb,tb,t);
    }

    private static int lerp(int a, int b, float t) { return a + (int)((b - a) * t); }

    // ─────────────────────────── events ─────────────────────────────────── //
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || hoveredBtn < 0) return super.mouseClicked(mx, my, button);
        switch (hoveredBtn) {
            case 0 -> client.setScreen(new SelectWorldScreen(this));
            case 1 -> client.setScreen(new MultiplayerScreen(this));
            case 2 -> client.setScreen(new AccountManagerScreen(this));
            case 3 -> client.setScreen(new OptionsScreen(this, client.options));
            case 4 -> client.scheduleStop();
        }
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}