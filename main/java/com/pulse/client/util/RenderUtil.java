package com.pulse.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class RenderUtil {

    public static void drawTracer(MatrixStack matrices, Entity entity, int color, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null || mc.player == null) return;

        Camera camera    = mc.gameRenderer.getCamera();
        Vec3d  camPos    = camera.getPos();
        Vec3d  entityPos = entity.getLerpedPos(tickDelta);

        float ex = (float)(entityPos.x - camPos.x);
        float ey = (float)(entityPos.y + entity.getHeight() * 0.5 - camPos.y);
        float ez = (float)(entityPos.z - camPos.z);

        float a = ((color >> 24) & 0xFF) / 255f; if (a < 0.01f) a = 1f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = (color         & 0xFF) / 255f;

        VertexConsumerProvider.Immediate immediate =
                mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer lines = immediate.getBuffer(RenderLayer.getLines());

        // normal() принимает MatrixStack.Entry, а не отдельную Matrix3f
        MatrixStack.Entry entry = matrices.peek();
        lines.vertex(entry, ex * 0, ey * 0, ez * 0).color(r, g, b, a).normal(entry, 0f, 1f, 0f);
        lines.vertex(entry, ex,     ey,     ez    ).color(r, g, b, a).normal(entry, 0f, 1f, 0f);

        immediate.draw(RenderLayer.getLines());
    }

    public static void drawEntityBoundingBox(MatrixStack matrices, Entity entity, int color, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d  camPos = camera.getPos();
        Vec3d  pos    = entity.getLerpedPos(tickDelta);

        float ex = (float)(pos.x - camPos.x);
        float ey = (float)(pos.y - camPos.y);
        float ez = (float)(pos.z - camPos.z);
        float hw = entity.getWidth()  * 0.5f;
        float ht = entity.getHeight();

        float a  = ((color >> 24) & 0xFF) / 255f; if (a < 0.01f) a = 1f;
        float r  = ((color >> 16) & 0xFF) / 255f;
        float gv = ((color >> 8)  & 0xFF) / 255f;
        float b  = (color         & 0xFF) / 255f;

        VertexConsumerProvider.Immediate immediate =
                mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer lines = immediate.getBuffer(RenderLayer.getLines());

        MatrixStack.Entry e = matrices.peek();

        // bottom
        addLine(lines, e, ex-hw, ey,    ez-hw, ex+hw, ey,    ez-hw, r,gv,b,a);
        addLine(lines, e, ex+hw, ey,    ez-hw, ex+hw, ey,    ez+hw, r,gv,b,a);
        addLine(lines, e, ex+hw, ey,    ez+hw, ex-hw, ey,    ez+hw, r,gv,b,a);
        addLine(lines, e, ex-hw, ey,    ez+hw, ex-hw, ey,    ez-hw, r,gv,b,a);
        // top
        addLine(lines, e, ex-hw, ey+ht, ez-hw, ex+hw, ey+ht, ez-hw, r,gv,b,a);
        addLine(lines, e, ex+hw, ey+ht, ez-hw, ex+hw, ey+ht, ez+hw, r,gv,b,a);
        addLine(lines, e, ex+hw, ey+ht, ez+hw, ex-hw, ey+ht, ez+hw, r,gv,b,a);
        addLine(lines, e, ex-hw, ey+ht, ez+hw, ex-hw, ey+ht, ez-hw, r,gv,b,a);
        // verticals
        addLine(lines, e, ex-hw, ey,    ez-hw, ex-hw, ey+ht, ez-hw, r,gv,b,a);
        addLine(lines, e, ex+hw, ey,    ez-hw, ex+hw, ey+ht, ez-hw, r,gv,b,a);
        addLine(lines, e, ex+hw, ey,    ez+hw, ex+hw, ey+ht, ez+hw, r,gv,b,a);
        addLine(lines, e, ex-hw, ey,    ez+hw, ex-hw, ey+ht, ez+hw, r,gv,b,a);

        immediate.draw(RenderLayer.getLines());
    }

    private static void addLine(VertexConsumer vc, MatrixStack.Entry e,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float r, float g, float b, float a) {
        float dx = x2-x1, dy = y2-y1, dz = z2-z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 0.001f) len = 1f;
        vc.vertex(e, x1, y1, z1).color(r, g, b, a).normal(e, dx/len, dy/len, dz/len);
        vc.vertex(e, x2, y2, z2).color(r, g, b, a).normal(e, dx/len, dy/len, dz/len);
    }
}