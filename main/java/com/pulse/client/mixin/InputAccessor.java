package com.pulse.client.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {

    // Получаем вектор
    @Accessor("movementVector")
    Vec2f getMovementVector();

    // Записываем вектор
    @Accessor("movementVector")
    void setMovementVector(Vec2f movementVector);
}