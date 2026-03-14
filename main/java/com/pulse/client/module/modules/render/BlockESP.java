package com.pulse.client.module.modules.render;

import org.lwjgl.opengl.GL11;
import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventRender3D;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * BlockESP — подсвечивает блоки ЧЕРЕЗ стены (Fabric 1.20.4).
 */
public class BlockESP extends Module {

    // ── Радиус ──────────────────────────────────────────────────────────────
    public final Setting<Integer> range = register(
            new Setting<>("Range", 4, "Радиус поиска (в чанках)").setRange(1, 10));

    // ── Цвет ────────────────────────────────────────────────────────────────
    public final Setting<Integer> colorR = register(
            new Setting<>("ColorR", 0,   "Красный (0–255)").setRange(0, 255));
    public final Setting<Integer> colorG = register(
            new Setting<>("ColorG", 191, "Зелёный (0–255)").setRange(0, 255));
    public final Setting<Integer> colorB = register(
            new Setting<>("ColorB", 255, "Синий (0–255)").setRange(0, 255));
    public final Setting<Integer> colorA = register(
            new Setting<>("ColorA", 255, "Прозрачность обводки (0–255)").setRange(0, 255));

    // ── Заливка ─────────────────────────────────────────────────────────────
    public final Setting<Boolean> fill = register(
            new Setting<>("Fill", true, "Полупрозрачная заливка"));
    public final Setting<Integer> fillAlpha = register(
            new Setting<>("FillAlpha", 35, "Прозрачность заливки (0–255)").setRange(0, 255));

    // ── Блоки ───────────────────────────────────────────────────────────────
    public final Setting<Boolean> diamonds      = register(new Setting<>("Diamonds",      true,  "Алмазная руда"));
    public final Setting<Boolean> ancientDebris = register(new Setting<>("AncientDebris", true,  "Ancient Debris"));
    public final Setting<Boolean> emeralds      = register(new Setting<>("Emeralds",      false, "Изумрудная руда"));
    public final Setting<Boolean> gold          = register(new Setting<>("Gold",          false, "Золотая руда"));
    public final Setting<Boolean> iron          = register(new Setting<>("Iron",          false, "Железная руда"));
    public final Setting<Boolean> copper        = register(new Setting<>("Copper",        false, "Медная руда"));
    public final Setting<Boolean> redstone      = register(new Setting<>("Redstone",      false, "Редстоун руда"));
    public final Setting<Boolean> lapis         = register(new Setting<>("Lapis",         false, "Лазуритовая руда"));
    public final Setting<Boolean> coal          = register(new Setting<>("Coal",          false, "Угольная руда"));
    public final Setting<Boolean> chests        = register(new Setting<>("Chests",        true,  "Сундуки"));
    public final Setting<Boolean> spawners      = register(new Setting<>("Spawners",      true,  "Спавнеры"));

    // ── Кэш ─────────────────────────────────────────────────────────────────
    private final List<BlockPos> cachedBlocks = new CopyOnWriteArrayList<>();
    private int tickTimer = 0;
    private static final int SCAN_INTERVAL = 40;

    public BlockESP() {
        super("BlockESP", "Подсвечивает блоки через стены", Category.RENDER);
    }

    @Override
    public void onEnable() {
        cachedBlocks.clear();
        tickTimer = SCAN_INTERVAL;
    }

    @Override
    public void onDisable() {
        cachedBlocks.clear();
    }

    // ── Обновление кэша раз в 2 сек ─────────────────────────────────────────
    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (++tickTimer < SCAN_INTERVAL) return;
        tickTimer = 0;

        List<Block> targets = buildTargetList();
        if (targets.isEmpty()) { cachedBlocks.clear(); return; }

        int r = range.getValue() * 16;
        BlockPos origin = mc.player.getBlockPos();
        List<BlockPos> found = new ArrayList<>();

        for (BlockPos pos : BlockPos.iterate(
                origin.add(-r, -r, -r),
                origin.add( r,  r,  r))) {
            Block block = mc.world.getBlockState(pos).getBlock();
            for (Block t : targets) {
                if (block == t) { found.add(pos.toImmutable()); break; }
            }
        }

        cachedBlocks.clear();
        cachedBlocks.addAll(found);
    }

    // ── Рендер ───────────────────────────────────────────────────────────────
    @EventHandler
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;
        if (cachedBlocks.isEmpty()) return;

        MatrixStack matrices = event.getMatrixStack();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        final float r  = colorR.getValue()    / 255f;
        final float g  = colorG.getValue()    / 255f;
        final float b  = colorB.getValue()    / 255f;
        final float a  = colorA.getValue()    / 255f;
        final float fa = fillAlpha.getValue() / 255f;
        final boolean doFill = fill.getValue();

        VertexConsumerProvider.Immediate immediate =
                mc.getBufferBuilders().getEntityVertexConsumers();

        // Отключаем depth test — рисуем ЧЕРЕЗ стены
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        for (BlockPos pos : cachedBlocks) {
            if (mc.world.getBlockState(pos).isAir()) continue;

            Box shape;
            try {
                shape = mc.world.getBlockState(pos)
                        .getOutlineShape(mc.world, pos)
                        .getBoundingBox();
            } catch (Exception ex) { continue; }

            double ox = pos.getX() + shape.minX - cam.x;
            double oy = pos.getY() + shape.minY - cam.y;
            double oz = pos.getZ() + shape.minZ - cam.z;
            double sx = shape.maxX - shape.minX;
            double sy = shape.maxY - shape.minY;
            double sz = shape.maxZ - shape.minZ;

            if (doFill) drawFilledBox(matrices, immediate, ox, oy, oz, sx, sy, sz, r, g, b, fa);
            drawBoxOutline(matrices, immediate, ox, oy, oz, sx, sy, sz, r, g, b, a);
        }

        // Сбрасываем буферы по нужным слоям
        immediate.draw(RenderLayer.getLines());
        if (doFill) immediate.draw(RenderLayer.getDebugFilledBox());

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    // ── Список блоков ────────────────────────────────────────────────────────
    private List<Block> buildTargetList() {
        List<Block> list = new ArrayList<>();
        if (diamonds.getValue())      { list.add(Blocks.DIAMOND_ORE);   list.add(Blocks.DEEPSLATE_DIAMOND_ORE); }
        if (ancientDebris.getValue())   list.add(Blocks.ANCIENT_DEBRIS);
        if (emeralds.getValue())      { list.add(Blocks.EMERALD_ORE);   list.add(Blocks.DEEPSLATE_EMERALD_ORE); }
        if (gold.getValue())          { list.add(Blocks.GOLD_ORE);      list.add(Blocks.DEEPSLATE_GOLD_ORE); list.add(Blocks.NETHER_GOLD_ORE); }
        if (iron.getValue())          { list.add(Blocks.IRON_ORE);      list.add(Blocks.DEEPSLATE_IRON_ORE); }
        if (copper.getValue())        { list.add(Blocks.COPPER_ORE);    list.add(Blocks.DEEPSLATE_COPPER_ORE); }
        if (redstone.getValue())      { list.add(Blocks.REDSTONE_ORE);  list.add(Blocks.DEEPSLATE_REDSTONE_ORE); }
        if (lapis.getValue())         { list.add(Blocks.LAPIS_ORE);     list.add(Blocks.DEEPSLATE_LAPIS_ORE); }
        if (coal.getValue())          { list.add(Blocks.COAL_ORE);      list.add(Blocks.DEEPSLATE_COAL_ORE); }
        if (chests.getValue())        { list.add(Blocks.CHEST);         list.add(Blocks.TRAPPED_CHEST); list.add(Blocks.ENDER_CHEST); }
        if (spawners.getValue())      { list.add(Blocks.SPAWNER);       list.add(Blocks.TRIAL_SPAWNER); }
        return list;
    }

    // ── Обводка 12 рёбер ─────────────────────────────────────────────────────
    private static void drawBoxOutline(MatrixStack matrices, VertexConsumerProvider.Immediate imm,
                                       double ox, double oy, double oz,
                                       double sx, double sy, double sz,
                                       float r, float g, float b, float a) {
        VertexConsumer vc = imm.getBuffer(RenderLayer.getLines());

        // В 1.20.4 берём матрицы из Entry явно
        Matrix4f posMat  = matrices.peek().getPositionMatrix();
        Matrix3f normMat = matrices.peek().getNormalMatrix();

        float x0 = (float) ox,        y0 = (float) oy,        z0 = (float) oz;
        float x1 = (float)(ox + sx),  y1 = (float)(oy + sy),  z1 = (float)(oz + sz);

        ln(vc, posMat, normMat, x0,y0,z0, x1,y0,z0, r,g,b,a);
        ln(vc, posMat, normMat, x1,y0,z0, x1,y0,z1, r,g,b,a);
        ln(vc, posMat, normMat, x1,y0,z1, x0,y0,z1, r,g,b,a);
        ln(vc, posMat, normMat, x0,y0,z1, x0,y0,z0, r,g,b,a);

        ln(vc, posMat, normMat, x0,y1,z0, x1,y1,z0, r,g,b,a);
        ln(vc, posMat, normMat, x1,y1,z0, x1,y1,z1, r,g,b,a);
        ln(vc, posMat, normMat, x1,y1,z1, x0,y1,z1, r,g,b,a);
        ln(vc, posMat, normMat, x0,y1,z1, x0,y1,z0, r,g,b,a);

        ln(vc, posMat, normMat, x0,y0,z0, x0,y1,z0, r,g,b,a);
        ln(vc, posMat, normMat, x1,y0,z0, x1,y1,z0, r,g,b,a);
        ln(vc, posMat, normMat, x1,y0,z1, x1,y1,z1, r,g,b,a);
        ln(vc, posMat, normMat, x0,y0,z1, x0,y1,z1, r,g,b,a);
    }

    /**
     * Одно ребро.
     * В 1.20.4: vertex(Matrix4f, f,f,f) / color(f,f,f,f) / normal(Matrix3f, f,f,f)
     */
    private static void ln(VertexConsumer vc,
                           Matrix4f posMat, Matrix3f normMat,
                           float x1, float y1, float z1,
                           float x2, float y2, float z2,
                           float r,  float g,  float b, float a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-4f) len = 1f;
        float nx = dx/len, ny = dy/len, nz = dz/len;

        vc.vertex(posMat, x1, y1, z1).color(r, g, b, a).normal(normMat, nx, ny, nz);
        vc.vertex(posMat, x2, y2, z2).color(r, g, b, a).normal(normMat, nx, ny, nz);
    }

    // ── Заливка ──────────────────────────────────────────────────────────────
    private static void drawFilledBox(MatrixStack matrices, VertexConsumerProvider.Immediate imm,
                                      double ox, double oy, double oz,
                                      double sx, double sy, double sz,
                                      float r, float g, float b, float a) {
        VertexConsumer vc = imm.getBuffer(RenderLayer.getDebugFilledBox());
        Matrix4f posMat = matrices.peek().getPositionMatrix();

        float x0 = (float) ox,        y0 = (float) oy,        z0 = (float) oz;
        float x1 = (float)(ox + sx),  y1 = (float)(oy + sy),  z1 = (float)(oz + sz);

        quad(vc, posMat, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1, r,g,b,a); // низ
        quad(vc, posMat, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, r,g,b,a); // верх
        quad(vc, posMat, x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0, r,g,b,a); // север
        quad(vc, posMat, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, r,g,b,a); // юг
        quad(vc, posMat, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, r,g,b,a); // запад
        quad(vc, posMat, x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1, r,g,b,a); // восток
    }

    private static void quad(VertexConsumer vc, Matrix4f posMat,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float r,  float g,  float b, float a) {
        vc.vertex(posMat, x0, y0, z0).color(r, g, b, a);
        vc.vertex(posMat, x1, y1, z1).color(r, g, b, a);
        vc.vertex(posMat, x2, y2, z2).color(r, g, b, a);
        vc.vertex(posMat, x3, y3, z3).color(r, g, b, a);
    }
}