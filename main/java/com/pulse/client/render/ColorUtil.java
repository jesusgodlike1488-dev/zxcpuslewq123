package com.pulse.client.render;

import java.awt.Color;

/**
 * Утилиты для работы с цветами ARGB.
 * Интерполяция, альфа-манипуляции, HSB, радуга, затемнение/осветление.
 */
public class ColorUtil {

    // ── Извлечение компонентов ──────────────────────────────────────── //

    public static int getAlpha(int color) { return (color >> 24) & 0xFF; }
    public static int getRed(int color)   { return (color >> 16) & 0xFF; }
    public static int getGreen(int color) { return (color >> 8) & 0xFF; }
    public static int getBlue(int color)  { return color & 0xFF; }

    public static float[] getRGBA(int color) {
        return new float[]{
                getRed(color) / 255f,
                getGreen(color) / 255f,
                getBlue(color) / 255f,
                getAlpha(color) / 255f
        };
    }

    // ── Сборка ──────────────────────────────────────────────────────── //

    public static int pack(int r, int g, int b, int a) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int pack(int r, int g, int b) {
        return pack(r, g, b, 255);
    }

    // ── Альфа ───────────────────────────────────────────────────────── //

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (clamp(alpha) << 24);
    }

    public static int withAlpha(int color, float alpha) {
        return withAlpha(color, (int) (alpha * 255));
    }

    public static int multiplyAlpha(int color, float factor) {
        return withAlpha(color, (int) (getAlpha(color) * factor));
    }

    // ── Интерполяция ────────────────────────────────────────────────── //

    public static int lerp(int from, int to, float t) {
        if (t <= 0f) return from;
        if (t >= 1f) return to;
        int a = (int) (getAlpha(from) + (getAlpha(to) - getAlpha(from)) * t);
        int r = (int) (getRed(from) + (getRed(to) - getRed(from)) * t);
        int g = (int) (getGreen(from) + (getGreen(to) - getGreen(from)) * t);
        int b = (int) (getBlue(from) + (getBlue(to) - getBlue(from)) * t);
        return pack(r, g, b, a);
    }

    // ── HSB / Радуга ────────────────────────────────────────────────── //

    public static int rainbow(float offset, float saturation, float brightness) {
        float hue = (float) ((System.currentTimeMillis() % 5000L) / 5000.0 + offset) % 1f;
        return Color.HSBtoRGB(hue, saturation, brightness) | 0xFF000000;
    }

    public static int fromHSB(float hue, float saturation, float brightness) {
        return Color.HSBtoRGB(hue, saturation, brightness) | 0xFF000000;
    }

    // ── Осветление / Затемнение ─────────────────────────────────────── //

    public static int brighter(int color, float factor) {
        return pack(
                Math.min(255, (int) (getRed(color) * (1 + factor))),
                Math.min(255, (int) (getGreen(color) * (1 + factor))),
                Math.min(255, (int) (getBlue(color) * (1 + factor))),
                getAlpha(color)
        );
    }

    public static int darker(int color, float factor) {
        return pack(
                Math.max(0, (int) (getRed(color) * (1 - factor))),
                Math.max(0, (int) (getGreen(color) * (1 - factor))),
                Math.max(0, (int) (getBlue(color) * (1 - factor))),
                getAlpha(color)
        );
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
