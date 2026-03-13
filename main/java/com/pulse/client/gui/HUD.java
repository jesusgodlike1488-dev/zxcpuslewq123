package com.pulse.client.gui;

import com.pulse.client.PulseClient;
import com.pulse.client.event.EventHandler;
import com.pulse.client.event.IListener;
import com.pulse.client.event.events.EventRender2D;
import com.pulse.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HUD implements IListener {

    private static final int TEXT_COLOR    = 0xFFFFFFFF;
    private static final int ACCENT_COLOR  = 0xFF1E90FF;
    private static final int BG_COLOR      = 0x99141414;

    // Cached sorted enabled list — avoids sort + allocation per frame
    private List<Module> sortedEnabled = new ArrayList<>();
    private int lastEnabledHash = 0;

    public HUD() {
        PulseClient.getInstance().getEventBus().register(this);
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null) return;

        DrawContext ctx = event.getDrawContext();
        TextRenderer font = mc.textRenderer;

        drawWatermark(ctx, font);
        drawArrayList(ctx, font, mc.getWindow().getScaledWidth());
    }

    private void drawWatermark(DrawContext ctx, TextRenderer font) {
        String name    = "§bPulse§fClient";
        String version = " §71.0.0";
        ctx.drawTextWithShadow(font, name + version, 4, 4, TEXT_COLOR);
    }

    private void drawArrayList(DrawContext ctx, TextRenderer font, int screenWidth) {
        List<Module> enabled = PulseClient.getInstance().getModuleManager().getEnabledModules();
        if (enabled.isEmpty()) {
            sortedEnabled.clear();
            lastEnabledHash = 0;
            return;
        }

        // Only re-sort when the enabled list actually changed
        int currentHash = enabled.hashCode();
        if (currentHash != lastEnabledHash || sortedEnabled.size() != enabled.size()) {
            sortedEnabled = new ArrayList<>(enabled);
            sortedEnabled.sort(Comparator.comparingInt(m -> -font.getWidth(m.getName())));
            lastEnabledHash = currentHash;
        }

        int y = 2;
        for (int i = 0, size = sortedEnabled.size(); i < size; i++) {
            Module module = sortedEnabled.get(i);
            String name  = module.getName();
            int textW    = font.getWidth(name);
            int x        = screenWidth - textW - 6;
            int bgX      = screenWidth - textW - 8;

            ctx.fill(bgX, y - 1, screenWidth, y + 10, BG_COLOR);
            ctx.fill(screenWidth - 2, y - 1, screenWidth, y + 10, ACCENT_COLOR);
            ctx.drawTextWithShadow(font, name, x, y + 1, TEXT_COLOR);
            y += 11;
        }
    }
}
