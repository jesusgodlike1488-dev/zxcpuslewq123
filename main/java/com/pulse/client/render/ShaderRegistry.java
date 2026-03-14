package com.pulse.client.render;

import com.pulse.client.PulseClient;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/**
 * Регистрация кастомных шейдеров через Fabric API.
 *
 * ⚠️ MC 1.20.4: CoreShaderRegistrationCallback вызывается при загрузке ресурсов.
 * Шейдер может стать null при горячей перезагрузке (F3+T) — всегда проверяй!
 *
 * JSON шейдера ОБЯЗАН содержать поле "vertex"/"fragment" (или "program") и
 * секцию "uniforms" со ВСЕМИ кастомными переменными — иначе MC их молча игнорирует.
 */
public class ShaderRegistry {

    // ⚠️ Может быть null во время горячей перезагрузки ресурсов!
    public static ShaderProgram UI_SDF;

    /**
     * Вызывать из onInitializeClient() — регистрирует callback.
     * Сам шейдер будет загружен позже, когда MC загрузит ресурсы.
     */
    public static void init() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            // ⚠️ MC 1.20.4: Identifier должен указывать на JSON в
            // assets/<namespace>/shaders/core/<path>.json
            Identifier shaderId = Identifier.of("pulseclient", "core/ui_sdf");

            context.register(
                    shaderId,
                    // VertexFormats.POSITION_TEXTURE — Position (vec3) + UV0 (vec2)
                    VertexFormats.POSITION_TEXTURE,
                    program -> {
                        // Callback вызывается после успешной компиляции шейдера.
                        // Null-check не нужен здесь — MC не вызовет callback при ошибке.
                        UI_SDF = program;
                        PulseClient.LOGGER.info("[ShaderRegistry] ui_sdf loaded successfully");
                    }
            );
        });
    }
}
