package com.pulse.client.module.modules.render;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventRender2D;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.util.WorldToScreenUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public class ItemESP extends Module {

    // Reusable projection result array — avoids per-entity allocation
    private final float[] screenPos = new float[2];

    public ItemESP() {
        super("ItemESP", "Показывает названия предметов на земле", Category.RENDER);
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.gameRenderer == null) return;

        DrawContext ctx = event.getDrawContext();

        for (ItemEntity itemEntity : mc.world.getEntitiesByClass(
                ItemEntity.class,
                mc.player.getBoundingBox().expand(48),
                e -> true)) {

            ItemStack stack = itemEntity.getStack();
            if (stack.isEmpty()) continue;

            Vec3d pos = itemEntity.getLerpedPos(event.getTickDelta())
                    .add(0, itemEntity.getHeight() + 0.35, 0);

            if (!WorldToScreenUtil.worldToScreen(pos, screenPos)) continue;

            int screenX = (int) screenPos[0];
            int screenY = (int) screenPos[1];

            String nameText = stack.getName().getString();
            String countText = stack.getCount() > 1 ? " x" + stack.getCount() : "";

            int nameWidth = mc.textRenderer.getWidth(nameText);
            int countWidth = countText.isEmpty() ? 0 : mc.textRenderer.getWidth(countText);
            int totalWidth = nameWidth + countWidth;
            int textHeight = mc.textRenderer.fontHeight;

            int x = screenX - totalWidth / 2;
            int y = screenY - textHeight / 2;

            // Темный полупрозрачный фон
            ctx.fill(x - 2, y - 1, x + totalWidth + 2, y + textHeight + 1, 0x90000000);

            // Название предмета (белый)
            ctx.drawTextWithShadow(mc.textRenderer, nameText, x, y, 0xFFFFFFFF);

            // Количество (красный)
            if (!countText.isEmpty()) {
                ctx.drawTextWithShadow(mc.textRenderer, countText, x + nameWidth, y, 0xFFFF5555);
            }
        }
    }
}
