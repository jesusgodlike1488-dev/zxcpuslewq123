package com.pulse.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

/**
 * Shader-based рендер API для UI.
 *
 * Рисует SDF-панели одним draw call: скруглённый прямоугольник с градиентом,
 * обводкой, тенью и SDF-клиппингом — всё на GPU без ветвлений.
 *
 * ⚠️ MC 1.20.4: GameRenderer сбрасывает стейты после каждого setShader,
 * поэтому все uniform-ы передаются заново при каждом вызове drawPanel().
 */
public class RenderUtils {

    /**
     * Рисует SDF-панель с градиентом, обводкой, тенью и клиппингом.
     *
     * PADDING НА ТЕНЬ:
     * Квад расширяется на shadowSoftness во все стороны, чтобы тень не обрезалась.
     * UV пересчитываются автоматически: шейдер получает totalSize = rectSize + 2*shadow,
     * и UV [0..1] покрывают весь расширенный квад. Внутри шейдера центр фигуры
     * вычисляется как totalSize/2, а u_RectSize задаёт реальный размер без padding.
     *
     * @param matrices    стек матриц (из DrawContext.getMatrices())
     * @param x           левый край фигуры (без padding) в scaled pixels
     * @param y           верхний край фигуры (без padding) в scaled pixels
     * @param w           ширина фигуры в scaled pixels
     * @param h           высота фигуры в scaled pixels
     * @param radius      радиус скругления углов в пикселях
     * @param borderWidth толщина внутренней обводки (0 = нет обводки)
     * @param shadowSoftness размытие тени (0 = нет тени); квад расширяется на это значение
     * @param colorTop    верхний цвет градиента (ARGB int)
     * @param colorBottom нижний цвет градиента (ARGB int)
     * @param borderColor цвет обводки (ARGB int)
     * @param shadowColor цвет тени (ARGB int, альфа управляет интенсивностью)
     * @param clipCX      X центра клип-зоны в пространстве квада (пиксели от левого края квада)
     * @param clipCY      Y центра клип-зоны в пространстве квада
     * @param clipW       ширина клип-зоны (полная, не полу-)
     * @param clipH       высота клип-зоны (полная, не полу-)
     * @param clipRadius  радиус скругления клип-зоны
     */
    public static void drawPanel(
            net.minecraft.client.util.math.MatrixStack matrices,
            float x, float y, float w, float h,
            float radius, float borderWidth, float shadowSoftness,
            int colorTop, int colorBottom,
            int borderColor, int shadowColor,
            float clipCX, float clipCY, float clipW, float clipH,
            float clipRadius
    ) {
        ShaderProgram shader = ShaderRegistry.UI_SDF;
        // ⚠️ null-check: шейдер может быть null при горячей перезагрузке ресурсов (F3+T)
        if (shader == null) return;

        // Включаем блендинг: srcAlpha, oneMinusSrcAlpha
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // ⚠️ MC 1.20.4: setShader принимает ShaderProgram напрямую (не Supplier)
        RenderSystem.setShader(shader);

        // ── Передача uniform-ов ──────────────────────────────────────
        // ⚠️ Все uniform-ы передаются ПОСЛЕ setShader — GameRenderer сбрасывает их

        setUniform2f(shader, "u_RectSize", w, h);
        setUniform1f(shader, "u_Radius", radius);
        setUniform4f(shader, "u_ColorTop", colorTop);
        setUniform4f(shader, "u_ColorBottom", colorBottom);
        setUniform4f(shader, "u_BorderColor", borderColor);
        setUniform1f(shader, "u_BorderWidth", borderWidth);
        setUniform4f(shader, "u_ShadowColor", shadowColor);
        setUniform1f(shader, "u_ShadowSoftness", shadowSoftness);
        setUniform4f(shader, "u_ClipRect", clipCX, clipCY, clipW, clipH);
        setUniform1f(shader, "u_ClipRadius", clipRadius);

        // ── Построение квада ─────────────────────────────────────────
        // Расширяем квад на shadowSoftness во все стороны для тени
        float pad = shadowSoftness;
        float qx  = x - pad;   // Левый край расширенного квада
        float qy  = y - pad;   // Верхний край расширенного квада
        float qw  = w + pad * 2; // Полная ширина с padding
        float qh  = h + pad * 2; // Полная высота с padding

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // ⚠️ MC 1.20.4: Tessellator.getInstance() → begin() → вершины → end()
        // VertexFormats.POSITION_TEXTURE = Position (vec3) + UV0 (vec2)
        BufferBuilder buf = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE
        );

        // UV (0,0) → (1,1): шейдер сам пересчитает в пиксели через totalSize
        // Порядок вершин: TL → BL → BR → TR (стандарт MC для QUADS)
        buf.vertex(matrix, qx,      qy,      0).texture(0f, 0f);
        buf.vertex(matrix, qx,      qy + qh, 0).texture(0f, 1f);
        buf.vertex(matrix, qx + qw, qy + qh, 0).texture(1f, 1f);
        buf.vertex(matrix, qx + qw, qy,      0).texture(1f, 0f);

        // ⚠️ MC 1.20.4: BufferRenderer.drawWithGlobalProgram() использует текущий setShader
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.disableBlend();
    }

    /**
     * Упрощённый вариант без клиппинга (клип-зона = бесконечность).
     */
    public static void drawPanel(
            net.minecraft.client.util.math.MatrixStack matrices,
            float x, float y, float w, float h,
            float radius, float borderWidth, float shadowSoftness,
            int colorTop, int colorBottom,
            int borderColor, int shadowColor
    ) {
        // Клип-центр в пространстве квада = центр самого квада,
        // размеры клипа = огромные → фактически нет клиппинга
        float pad = shadowSoftness;
        float totalW = w + pad * 2;
        float totalH = h + pad * 2;
        drawPanel(matrices, x, y, w, h, radius, borderWidth, shadowSoftness,
                colorTop, colorBottom, borderColor, shadowColor,
                totalW * 0.5f, totalH * 0.5f, 99999f, 99999f, 0f);
    }

    /**
     * Минимальный вариант: цвет + радиус, без обводки/тени/клиппинга.
     */
    public static void drawPanel(
            net.minecraft.client.util.math.MatrixStack matrices,
            float x, float y, float w, float h,
            float radius, int color
    ) {
        drawPanel(matrices, x, y, w, h, radius, 0f, 0f,
                color, color, 0x00000000, 0x00000000);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Утилиты для передачи uniform-ов
    // ══════════════════════════════════════════════════════════════════

    /** Передаёт float uniform. Null-safe: если uniform не найден — пропускаем. */
    private static void setUniform1f(ShaderProgram shader, String name, float v) {
        var u = shader.getUniform(name);
        if (u != null) u.set(v);
    }

    /** Передаёт vec2 uniform. */
    private static void setUniform2f(ShaderProgram shader, String name, float x, float y) {
        var u = shader.getUniform(name);
        if (u != null) u.set(x, y);
    }

    /** Передаёт vec4 uniform из ARGB int цвета. Конвертирует в нормализованные float [0..1]. */
    private static void setUniform4f(ShaderProgram shader, String name, int argb) {
        var u = shader.getUniform(name);
        if (u == null) return;
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8)  & 0xFF) / 255f;
        float b = (argb         & 0xFF) / 255f;
        u.set(r, g, b, a);
    }

    /** Передаёт vec4 uniform из 4 float значений. */
    private static void setUniform4f(ShaderProgram shader, String name,
                                     float x, float y, float z, float w) {
        var u = shader.getUniform(name);
        if (u != null) u.set(x, y, z, w);
    }
}
