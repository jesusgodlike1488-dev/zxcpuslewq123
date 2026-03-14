package com.pulse.client.gui.font;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class AWTFontRenderer {

    private static int instanceCounter = 0;

    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private final Font font;

    private Identifier textureId;
    private int fontHeight;

    private static final float SCALE    = 2f;
    private static final int ATLAS_SIZE = 2048;

    private final int atlasWidth  = ATLAS_SIZE;
    private final int atlasHeight = ATLAS_SIZE;

    // Храним пиксели до момента загрузки на GPU
    private int[] pendingPixels = null;
    private boolean uploaded = false;

    public AWTFontRenderer(Font font, String charsToLoad) {
        this.font = font;
        generateAtlasCPU(charsToLoad); // только CPU, без GL
    }

    // ── Шаг 1: генерация атласа на CPU (можно в любом потоке) ───────────────
    private void generateAtlasCPU(String chars) {
        BufferedImage atlasImage = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlasImage.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);

        Font bigFont = font.deriveFont(font.getSize2D() * SCALE);
        g.setFont(bigFont);
        g.setColor(Color.WHITE);

        FontMetrics metrics = g.getFontMetrics();

        int x = 0;
        int y = metrics.getAscent();

        for (int i = 0, len = chars.length(); i < len; i++) {
            char c = chars.charAt(i);
            int advance = metrics.charWidth(c);
            int slotW   = advance + 2;
            int h       = metrics.getHeight();

            if (x + slotW >= atlasWidth) {
                x = 0;
                y += h;
            }

            g.drawString(String.valueOf(c), x, y);
            glyphs.put(c, new Glyph(x, y - metrics.getAscent(), slotW, h, advance));
            x += slotW;
        }

        g.dispose();

        java.awt.font.FontRenderContext frc =
                new java.awt.font.FontRenderContext(null, true, true);
        fontHeight = (int)(bigFont.getLineMetrics("Ag", frc).getHeight() / SCALE);

        // Конвертируем пиксели в ABGR и сохраняем — без GL!
        pendingPixels = new int[atlasWidth * atlasHeight];
        for (int py = 0; py < atlasHeight; py++) {
            for (int px = 0; px < atlasWidth; px++) {
                int argb = atlasImage.getRGB(px, py);
                int a  = (argb >> 24) & 0xFF;
                int r  = (argb >> 16) & 0xFF;
                int g2 = (argb >>  8) & 0xFF;
                int b  = (argb      ) & 0xFF;
                // NativeImage формат ABGR
                pendingPixels[py * atlasWidth + px] = (a << 24) | (b << 16) | (g2 << 8) | r;
            }
        }

        textureId = new Identifier("pulseclient", "font_atlas_" + instanceCounter++);
    }

    // ── Шаг 2: загрузка на GPU (только render thread) ────────────────────────
    private void uploadToGPU() {
        if (uploaded || pendingPixels == null) return;
        uploaded = true;

        NativeImage nativeImage = new NativeImage(atlasWidth, atlasHeight, false);
        for (int py = 0; py < atlasHeight; py++) {
            for (int px = 0; px < atlasWidth; px++) {
                nativeImage.setColor(px, py, pendingPixels[py * atlasWidth + px]);
            }
        }
        pendingPixels = null; // освобождаем память

        NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
        texture.upload();
        MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);

        RenderSystem.bindTexture(texture.getGlId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    // ── Рендер ───────────────────────────────────────────────────────────────
    public void drawString(DrawContext ctx, String text, float x, float y, int color) {
        if (text == null || text.isEmpty() || textureId == null) return;

        // Загружаем текстуру при первом рендере (гарантированно render thread)
        if (!uploaded) uploadToGPU();

        ctx.getMatrices().push();
        ctx.getMatrices().scale(1f / SCALE, 1f / SCALE, 1f);

        float currentX = x * SCALE;
        float currentY = y * SCALE;

        int alpha = (color >> 24) & 0xFF;
        if (alpha == 0) alpha = 255; // если альфа не задана — непрозрачный

        float a = alpha / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = (color         & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(r, g, b, a);

        for (int i = 0, len = text.length(); i < len; i++) {
            Glyph glyph = glyphs.get(text.charAt(i));
            if (glyph == null) continue;

            ctx.drawTexture(
                    textureId,
                    Math.round(currentX), Math.round(currentY),
                    (float) glyph.x, (float) glyph.y,
                    glyph.slotW, glyph.height,
                    atlasWidth, atlasHeight
            );

            currentX += glyph.advance;
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        ctx.getMatrices().pop();
    }

    public void drawStringWithShadow(DrawContext ctx, String text, float x, float y, int color) {
        drawString(ctx, text, x + 1, y + 1, 0xAA000000);
        drawString(ctx, text, x, y, color);
    }

    public void drawCenteredString(DrawContext ctx, String text, float x, float y, int color) {
        drawString(ctx, text, x - getWidth(text) / 2f, y, color);
    }

    public void drawCenteredStringWithShadow(DrawContext ctx, String text, float x, float y, int color) {
        drawStringWithShadow(ctx, text, x - getWidth(text) / 2f, y, color);
    }

    public int getStringWidth(String text) { return getWidth(text); }

    public int getWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        int width = 0;
        for (int i = 0, len = text.length(); i < len; i++) {
            Glyph glyph = glyphs.get(text.charAt(i));
            if (glyph != null) width += Math.round(glyph.advance / SCALE);
        }
        return width;
    }

    public int getHeight() { return fontHeight; }

    private static class Glyph {
        final int x, y, slotW, height, advance;
        Glyph(int x, int y, int slotW, int height, int advance) {
            this.x = x; this.y = y;
            this.slotW = slotW; this.height = height;
            this.advance = advance;
        }
    }
}