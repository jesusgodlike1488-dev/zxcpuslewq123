package com.pulse.client.render;

import net.minecraft.client.gui.DrawContext;

/**
 * 100% стабильный 2D рендер для Fabric 1.21.x.
 * Использует scanline-алгоритм через стандартный ctx.fill().
 * Идеальные закругления без багов прозрачности и наложений.
 * Не требует устаревших классов RenderSystem и Tessellator.
 */
public class Render2DEngine {

    // ══════════════════════════════════════════════════════════════
    //  СКРУГЛЁННЫЙ ПРЯМОУГОЛЬНИК (Без багов прозрачности)
    // ══════════════════════════════════════════════════════════════

    public static void drawRoundedRect(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.round(w), ih = Math.round(h);
        int ir = (int) Math.min(Math.max(r, 0), Math.min(iw, ih) / 2f);

        if (iw <= 0 || ih <= 0 || (color >>> 24) == 0) return;

        if (ir <= 0) {
            ctx.fill(ix, iy, ix + iw, iy + ih, color);
            return;
        }

        int rgb = color & 0x00FFFFFF;
        int alpha = (color >>> 24) & 0xFF;

        // 1. Сплошной центральный блок (Без наложений на углы)
        ctx.fill(ix, iy + ir, ix + iw, iy + ih - ir, color);

        float fr = ir + 0.5f;

        // 2. Отрисовка закруглений (сверху и снизу) горизонтальными линиями
        for (int dy = 0; dy < ir; dy++) {
            double exact = Math.sqrt(Math.max(0, fr * fr - (dy + 0.5) * (dy + 0.5)));
            int filled = (int) exact;

            // Вычисление сглаживания для крайних пикселей
            int aaAlpha = (int) (alpha * (exact - filled));
            int aaColor = (aaAlpha << 24) | rgb;

            int rowTop = iy + ir - 1 - dy; // Строка сверху
            int rowBot = iy + ih - ir + dy; // Строка снизу

            int leftSolid = ix + ir - filled;
            int rightSolid = ix + iw - ir + filled;

            // --- ВЕРХНЯЯ ЧАСТЬ ---
            ctx.fill(leftSolid, rowTop, rightSolid, rowTop + 1, color);
            if (aaAlpha > 0) {
                ctx.fill(leftSolid - 1, rowTop, leftSolid, rowTop + 1, aaColor);
                ctx.fill(rightSolid, rowTop, rightSolid + 1, rowTop + 1, aaColor);
            }

            // --- НИЖНЯЯ ЧАСТЬ ---
            ctx.fill(leftSolid, rowBot, rightSolid, rowBot + 1, color);
            if (aaAlpha > 0) {
                ctx.fill(leftSolid - 1, rowBot, leftSolid, rowBot + 1, aaColor);
                ctx.fill(rightSolid, rowBot, rightSolid + 1, rowBot + 1, aaColor);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ОБВОДКА
    // ══════════════════════════════════════════════════════════════

    public static void drawRoundedRectOutline(DrawContext ctx, float x, float y, float w, float h, float r, float lw, int color) {
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.round(w), ih = Math.round(h);
        int ilw = Math.max(1, Math.round(lw));
        int ir = (int) Math.min(Math.max(r, 0), Math.min(iw, ih) / 2f);

        if (iw <= 0 || ih <= 0 || (color >>> 24) == 0) return;

        // Прямые линии (левая, правая, верхняя, нижняя)
        ctx.fill(ix, iy + ir, ix + ilw, iy + ih - ir, color); // Лево
        ctx.fill(ix + iw - ilw, iy + ir, ix + iw, iy + ih - ir, color); // Право
        ctx.fill(ix + ir, iy, ix + iw - ir, iy + ilw, color); // Верх
        ctx.fill(ix + ir, iy + ih - ilw, ix + iw - ir, iy + ih, color); // Низ

        if (ir <= 0) return;

        float frO = ir + 0.5f;
        float frI = Math.max(0, ir - ilw) + 0.5f;

        for (int dy = 0; dy < ir; dy++) {
            double sampleY = dy + 0.5;
            double exactO = Math.sqrt(Math.max(0, frO * frO - sampleY * sampleY));
            double exactI = Math.sqrt(Math.max(0, frI * frI - sampleY * sampleY));

            int dxO = (int) exactO;
            int dxI = (int) exactI;
            if (dxO <= dxI) continue;

            int rowTop = iy + ir - 1 - dy;
            int rowBot = iy + ih - ir + dy;

            // Отрисовка пикселей обводки на углах
            // Левая сторона
            ctx.fill(ix + ir - dxO, rowTop, ix + ir - dxI, rowTop + 1, color); // Top-Left
            ctx.fill(ix + ir - dxO, rowBot, ix + ir - dxI, rowBot + 1, color); // Bot-Left
            // Правая сторона
            ctx.fill(ix + iw - ir + dxI, rowTop, ix + iw - ir + dxO, rowTop + 1, color); // Top-Right
            ctx.fill(ix + iw - ir + dxI, rowBot, ix + iw - ir + dxO, rowBot + 1, color); // Bot-Right
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  КРУГ
    // ══════════════════════════════════════════════════════════════

    public static void drawCircle(DrawContext ctx, float cx, float cy, float radius, int color) {
        // Оптимизация: круг - это просто скругленный квадрат, радиус которого равен половине ширины
        drawRoundedRect(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }

    // ══════════════════════════════════════════════════════════════
    //  ПРОСТЫЕ ФИГУРЫ
    // ══════════════════════════════════════════════════════════════

    public static void drawHLine(DrawContext ctx, float x, float y, float w, float t, int c) {
        ctx.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + t), c);
    }

    public static void drawVLine(DrawContext ctx, float x, float y, float h, float t, int c) {
        ctx.fill(Math.round(x), Math.round(y), Math.round(x + t), Math.round(y + h), c);
    }

    public static void drawBlurBackground(DrawContext ctx, int sw, int sh, float alpha) {
        int a = (int) (180 * alpha);
        // Безопасная упаковка цвета формата 0xAARRGGBB
        int color = (a << 24) | (3 << 16) | (3 << 8) | 8;
        ctx.fill(0, 0, sw, sh, color);
    }

    public static void drawGradientRect(DrawContext ctx, float x, float y, float w, float h, int top, int bot) {
        ctx.fillGradient(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), top, bot);
    }

    public static void drawProgressBar(DrawContext ctx, float x, float y, float w, float h, float r, float prog, int track, int fill) {
        prog = Math.max(0, Math.min(1, prog));

        // Рисуем задний фон
        drawRoundedRect(ctx, x, y, w, h, r, track);

        if (prog > 0.01f) {
            // Используем Scissor, чтобы обрезать правый край прогресса, сохраняя идеальные левые углы
            ctx.enableScissor((int)x, (int)y, (int)(x + w * prog), (int)(y + h));
            drawRoundedRect(ctx, x, y, w, h, r, fill);
            ctx.disableScissor();
        }
    }
}