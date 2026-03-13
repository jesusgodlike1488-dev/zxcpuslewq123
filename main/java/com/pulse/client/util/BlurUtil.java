package com.pulse.client.util;

import net.minecraft.client.gui.DrawContext;

/**
 * Rounded-rect drawing utility.
 * Optimized: scanline-based corner fill — one ctx.fill() per scanline row
 * instead of per-pixel. For radius=5, ~5 fill calls vs ~20 per corner.
 */
public class BlurUtil {

    // ─── full rounded rect ─────────────────────────────────────────────── //

    public static void drawRoundedRect(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        int ix = (int)x, iy = (int)y, iw = (int)w, ih = (int)h;
        int ir = (int) Math.min(r, Math.min(iw, ih) / 2f);
        if (iw <= 0 || ih <= 0) return;

        ctx.fill(ix + ir, iy,        ix + iw - ir, iy + ih,      color);
        ctx.fill(ix,      iy + ir,   ix + ir,      iy + ih - ir, color);
        ctx.fill(ix + iw - ir, iy + ir, ix + iw,  iy + ih - ir, color);

        fillCorner(ctx, ix + ir,      iy + ir,      ir, color, 2);
        fillCorner(ctx, ix + iw - ir, iy + ir,      ir, color, 1);
        fillCorner(ctx, ix + ir,      iy + ih - ir, ir, color, 3);
        fillCorner(ctx, ix + iw - ir, iy + ih - ir, ir, color, 0);
    }

    // ─── top-only rounded rect (for panel headers) ─────────────────────── //

    public static void drawRoundedRectTop(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        int ix = (int)x, iy = (int)y, iw = (int)w, ih = (int)h;
        int ir = (int) Math.min(r, Math.min(iw, ih) / 2f);
        if (iw <= 0 || ih <= 0) return;

        ctx.fill(ix + ir, iy,      ix + iw - ir, iy + ih, color);
        ctx.fill(ix,      iy + ir, ix + ir,      iy + ih, color);
        ctx.fill(ix + iw - ir, iy + ir, ix + iw, iy + ih, color);

        fillCorner(ctx, ix + ir,      iy + ir, ir, color, 2);
        fillCorner(ctx, ix + iw - ir, iy + ir, ir, color, 1);
    }

    // ─── 1px border outline ────────────────────────────────────────────── //

    public static void drawRoundedRectOutline(DrawContext ctx, float x, float y, float w, float h, float r, float lw, int color) {
        int ix = (int)x, iy = (int)y, iw = (int)w, ih = (int)h;
        int ilw = Math.max(1, (int)lw);
        int ir  = (int) Math.min(r, Math.min(iw, ih) / 2f);

        ctx.fill(ix + ir, iy,             ix + iw - ir, iy + ilw,            color); // top
        ctx.fill(ix + ir, iy + ih - ilw,  ix + iw - ir, iy + ih,             color); // bottom
        ctx.fill(ix,      iy + ir,        ix + ilw,      iy + ih - ir,        color); // left
        ctx.fill(ix + iw - ilw, iy + ir,  ix + iw,       iy + ih - ir,        color); // right
    }

    // ─── vertical gradient ─────────────────────────────────────────────── //

    public static void drawGradientRect(DrawContext ctx, int x, int y, int w, int h, int colorTop, int colorBottom) {
        ctx.fillGradient(x, y, x + w, y + h, colorTop, colorBottom);
    }

    // ─── OPTIMIZED: scanline-based quarter-circle fill ─────────────────── //

    /** quadrant: 0=bottom-right  1=bottom-left  2=top-left  3=top-right */
    private static void fillCorner(DrawContext ctx, int cx, int cy, int r, int color, int quad) {
        if (r <= 0) return;

        int rSq = r * r;

        for (int dy = 0; dy <= r; dy++) {
            // Horizontal extent for this scanline row
            int dx = (int) Math.sqrt(rSq - dy * dy);
            if (dx <= 0) continue;

            int x1, x2, y1, y2;

            switch (quad) {
                case 0: // bottom-right
                    x1 = cx; x2 = cx + dx;
                    y1 = cy + dy; y2 = y1 + 1;
                    break;
                case 1: // bottom-left
                    x1 = cx - dx; x2 = cx;
                    y1 = cy + dy; y2 = y1 + 1;
                    break;
                case 2: // top-left
                    x1 = cx - dx; x2 = cx;
                    y1 = cy - dy; y2 = y1 + 1;
                    break;
                case 3: // top-right
                    x1 = cx + 0; x2 = cx + dx;
                    y1 = cy - dy; y2 = y1 + 1;
                    break;
                default:
                    continue;
            }

            ctx.fill(x1, y1, x2, y2, color);
        }
    }
}
