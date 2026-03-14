package com.pulse.client.render;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Scissor (clipping) stack utility.
 * Allows nested scissor regions — each push intersects with the parent region.
 * Essential for scrollable panels and clipped content areas.
 */
public class ScissorUtil {

    private static final Deque<int[]> STACK = new ArrayDeque<>();

    /**
     * Push a scissor region (in scaled Minecraft coordinates).
     * If there's already a region, the new one is intersected with the parent.
     */
    public static void push(float x, float y, float width, float height) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double scale = mc.getWindow().getScaleFactor();

        int sx = (int) (x * scale);
        int sy = (int) ((mc.getWindow().getScaledHeight() - y - height) * scale);
        int sw = (int) (width * scale);
        int sh = (int) (height * scale);

        // Intersect with parent scissor if any
        if (!STACK.isEmpty()) {
            int[] parent = STACK.peek();
            int px2 = parent[0] + parent[2];
            int py2 = parent[1] + parent[3];
            int nx2 = sx + sw;
            int ny2 = sy + sh;

            sx = Math.max(sx, parent[0]);
            sy = Math.max(sy, parent[1]);
            sw = Math.max(0, Math.min(nx2, px2) - sx);
            sh = Math.max(0, Math.min(ny2, py2) - sy);
        }

        STACK.push(new int[]{sx, sy, sw, sh});
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);
    }

    /**
     * Pop the current scissor region and restore the parent's.
     */
    public static void pop() {
        if (STACK.isEmpty()) return;
        STACK.pop();

        if (STACK.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            int[] parent = STACK.peek();
            GL11.glScissor(parent[0], parent[1], parent[2], parent[3]);
        }
    }

    /**
     * Clear all scissor regions. Safety call for error recovery.
     */
    public static void clearAll() {
        STACK.clear();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}