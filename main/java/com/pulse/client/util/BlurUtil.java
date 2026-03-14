package com.pulse.client.util;

import com.pulse.client.render.Render2DEngine;
import net.minecraft.client.gui.DrawContext;

/**
 * Утилита скруглённых прямоугольников (для MainMenuScreen и AccountManagerScreen).
 * Делегирует в Render2DEngine.
 */
public class BlurUtil {

    public static void drawRoundedRect(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        Render2DEngine.drawRoundedRect(ctx, x, y, w, h, r, color);
    }

    public static void drawRoundedRectTop(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        int ix = (int) x, iy = (int) y, iw = (int) w, ih = (int) h;
        int ir = (int) Math.min(r, Math.min(iw, ih) / 2f);
        if (iw <= 0 || ih <= 0) return;

        ctx.fill(ix + ir, iy, ix + iw - ir, iy + ih, color);
        ctx.fill(ix, iy + ir, ix + ir, iy + ih, color);
        ctx.fill(ix + iw - ir, iy + ir, ix + iw, iy + ih, color);

        fillCorner(ctx, ix + ir, iy + ir, ir, color, 2);
        fillCorner(ctx, ix + iw - ir, iy + ir, ir, color, 1);
    }

    public static void drawRoundedRectOutline(DrawContext ctx, float x, float y, float w, float h, float r, float lw, int color) {
        Render2DEngine.drawRoundedRectOutline(ctx, x, y, w, h, r, lw, color);
    }

    public static void drawGradientRect(DrawContext ctx, int x, int y, int w, int h, int colorTop, int colorBottom) {
        ctx.fillGradient(x, y, x + w, y + h, colorTop, colorBottom);
    }

    private static void fillCorner(DrawContext ctx, int cx, int cy, int r, int color, int quad) {
        if (r <= 0) return;
        for (int dy = 0; dy <= r; dy++) {
            int dx = (int) Math.sqrt((double) r * r - (double) dy * dy);
            int x1, x2, y1;
            switch (quad) {
                case 0 -> { x1 = cx; x2 = cx + dx; y1 = cy + dy; }
                case 1 -> { x1 = cx; x2 = cx + dx; y1 = cy - dy; }
                case 2 -> { x1 = cx - dx; x2 = cx; y1 = cy - dy; }
                case 3 -> { x1 = cx - dx; x2 = cx; y1 = cy + dy; }
                default -> { continue; }
            }
            ctx.fill(x1, y1, x2, y1 + 1, color);
        }
    }
}
