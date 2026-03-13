package com.pulse.client.util;

import net.minecraft.entity.Entity;

public class RotationUtil {

    // true когда есть цель — yaw спуфится весь тик
    public static boolean active = false;

    // true когда нужно ударить в этот тик
    public static boolean attackThisTick = false;

    // true = снять спринт вокруг атаки (в воздухе — всегда)
    public static boolean shouldUnsprint = false;

    // Серверные ротации
    public static float aimYaw = 0f;
    public static float aimPitch = 0f;

    // Цель для удара
    public static Entity target = null;

    // Режим камеры — миксин читает для восстановления
    public static String focusMode = "Free";

    public static void reset() {
        active = false;
        attackThisTick = false;
        shouldUnsprint = false;
        target = null;
        focusMode = "Free";
    }
}