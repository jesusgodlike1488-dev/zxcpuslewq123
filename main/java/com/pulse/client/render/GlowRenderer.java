package com.pulse.client.render;

import net.minecraft.client.gui.DrawContext;

/**
 * Свечение и тени. Сильные, видимые эффекты.
 */
public class GlowRenderer {

    /**
     * Мощное свечение вокруг прямоугольника.
     * @param intensity 0..1
     * @param layers    количество слоёв (4-8)
     */
    public static void drawRectGlow(DrawContext ctx, float x, float y, float w, float h,
                                     float radius, int color, float intensity, int layers) {
        if (intensity < 0.01f || layers <= 0) return;
        int r = ColorUtil.getRed(color);
        int g = ColorUtil.getGreen(color);
        int b = ColorUtil.getBlue(color);

        for (int i = layers; i >= 1; i--) {
            float expand = i * 3f * intensity;
            // Прогрессивное затухание: ближние слои ярче
            float t = (float) i / layers;
            int alpha = (int) (55 * intensity * t);
            if (alpha <= 0) continue;
            int col = ColorUtil.pack(r, g, b, alpha);
            Render2DEngine.drawRoundedRect(ctx,
                    x - expand, y - expand,
                    w + expand * 2, h + expand * 2,
                    radius + expand * 0.5f, col);
        }
    }

    /**
     * Тень под карточкой.
     */
    public static void drawCardShadow(DrawContext ctx, float x, float y, float w, float h, float radius) {
        for (int i = 6; i >= 1; i--) {
            float expand = i * 2f;
            int alpha = 3 + i * 5;
            Render2DEngine.drawRoundedRect(ctx,
                    x - expand * 0.2f, y + expand * 0.3f,
                    w + expand * 0.4f, h + expand * 0.4f,
                    radius + expand * 0.3f,
                    ColorUtil.pack(0, 0, 0, alpha));
        }
    }

    /**
     * Индикатор в сайдбаре.
     */
    public static void drawSidebarIndicator(DrawContext ctx, float x, float y, float w, float h,
                                             int color, float intensity) {
        if (intensity < 0.01f) return;
        for (int i = 3; i >= 1; i--) {
            float expand = i * 2f * intensity;
            int alpha = (int) (20 * intensity);
            ctx.fill((int) (x - expand), (int) (y - expand / 2),
                    (int) (x + w + expand), (int) (y + h + expand / 2),
                    ColorUtil.withAlpha(color, alpha));
        }
        Render2DEngine.drawRoundedRect(ctx, x, y, w, h, w / 2f,
                ColorUtil.withAlpha(color, (int) (255 * intensity)));
    }
}
