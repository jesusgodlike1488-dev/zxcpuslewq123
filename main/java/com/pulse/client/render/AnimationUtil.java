package com.pulse.client.render;

/**
 * FPS-независимые анимации.
 */
public class AnimationUtil {

    private static long lastMs = System.currentTimeMillis();
    private static long lastFrame = 0;
    private static float realDelta = 0.016f;

    /**
     * Обновляет дельту ОДИН РАЗ за кадр.
     * Повторные вызовы в том же кадре не обнуляют realDelta.
     */
    private static void tick() {
        long now = System.currentTimeMillis();
        if (now - lastFrame < 1) return; // Уже обновили в этом кадре
        lastFrame = now;
        realDelta = (now - lastMs) / 1000f;
        if (realDelta > 0.05f) realDelta = 0.05f;
        if (realDelta < 0.001f) realDelta = 0.001f;
        lastMs = now;
    }

    /**
     * Экспоненциальное сглаживание. Плавно двигает current → target.
     * speed: 0.08 медленно, 0.2 средне, 0.4 быстро.
     */
    public static float animate(float current, float target, float speed, float ignoredDelta) {
        tick();
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) return target;
        return current + diff * (1f - (float) Math.exp(-speed * 40f * realDelta));
    }

    /** Линейная интерполяция с зажимом. */
    public static float lerp(float a, float b, float t) {
        float diff = b - a;
        if (Math.abs(diff) < 0.001f) return b;
        return a + diff * Math.min(1f, t);
    }

    public static float easeOutCubic(float t) {
        t = Math.max(0, Math.min(1, t));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    public static float easeOutQuad(float t) {
        t = Math.max(0, Math.min(1, t));
        return 1f - (1f - t) * (1f - t);
    }
}
