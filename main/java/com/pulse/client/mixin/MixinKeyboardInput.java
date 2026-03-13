package com.pulse.client.mixin;

import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Коррекция WASD больше не нужна.
 *
 * Раньше: yaw камеры != серверный yaw → нужно было пересчитывать WASD.
 * Теперь: yaw подменяется на aimYaw НА ВЕСЬ ТИК (в MixinClientPlayerEntity tick HEAD).
 * travel() видит yaw = aimYaw → двигает игрока правильно → коррекция не нужна.
 */
@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
    // Пустой — коррекция WASD убрана
}