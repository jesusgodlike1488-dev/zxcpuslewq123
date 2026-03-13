package com.pulse.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Shared world-to-screen projection utility.
 * Reusable Vector4f avoids per-entity heap allocation.
 * Matrices are rebuilt each call — the cost (~2 matrix ops) is negligible.
 */
public class WorldToScreenUtil {

    private static final Matrix4f viewMatrix = new Matrix4f();
    private static final Matrix4f projMatrix = new Matrix4f();
    private static final Vector4f tempVec = new Vector4f();

    /**
     * Projects a world position to screen coordinates.
     *
     * @param worldPos  World position to project
     * @param result    float[2] array to store screenX, screenY. Pass a reusable array to avoid allocation.
     * @return true if projection succeeded and is on screen
     */
    public static boolean worldToScreen(Vec3d worldPos, float[] result) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null || mc.player == null) return false;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        Window window = mc.getWindow();

        float pitch = (float) Math.toRadians(camera.getPitch());
        float yaw   = (float) Math.toRadians(camera.getYaw() + 180.0f);
        viewMatrix.identity().rotateX(pitch).rotateY(yaw);
        projMatrix.set(mc.gameRenderer.getBasicProjectionMatrix(mc.options.getFov().getValue()));

        float rx = (float)(worldPos.x - camPos.x);
        float ry = (float)(worldPos.y - camPos.y);
        float rz = (float)(worldPos.z - camPos.z);

        tempVec.set(rx, ry, rz, 1.0f);
        viewMatrix.transform(tempVec);
        projMatrix.transform(tempVec);

        if (tempVec.w <= 0) return false;

        float ndcX = tempVec.x / tempVec.w;
        float ndcY = tempVec.y / tempVec.w;

        if (ndcX < -1.2f || ndcX > 1.2f || ndcY < -1.2f || ndcY > 1.2f) return false;

        result[0] = (ndcX + 1.0f) / 2.0f * window.getScaledWidth();
        result[1] = (1.0f - ndcY) / 2.0f * window.getScaledHeight();
        return true;
    }
}
