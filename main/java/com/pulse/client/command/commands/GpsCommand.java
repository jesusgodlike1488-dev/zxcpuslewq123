package com.pulse.client.command.commands;

import com.pulse.client.PulseClient;
import com.pulse.client.command.Command;
import com.pulse.client.event.EventHandler;
import com.pulse.client.event.IListener;
import com.pulse.client.event.events.EventRender2D;
import com.pulse.client.gui.font.AWTFontRenderer;
import com.pulse.client.gui.font.FontManager;
import com.pulse.client.util.WorldToScreenUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class GpsCommand extends Command implements IListener {

    // Состояние метки
    private boolean active = false;
    private int targetX = 0;
    private int targetZ = 0;

    // Массив для проекции 3D координат в 2D (экран)
    private final float[] screenPos = new float[2];

    public GpsCommand() {
        super("gps", "Ставит 2D метку на экране", "gps <x> <z> | gps clear");

        // ВАЖНО: Подписываем саму команду на ивенты, чтобы она могла рисовать (Render2D)
        // Замени Pulse.eventManager на твой менеджер ивентов, если он называется иначе
        PulseClient.getInstance().getEventBus().register(this);
    }

    @Override
    public void execute(String[] args) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Если ввели: .gps clear
        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            active = false;
            sendMessage("§c[GPS] Метка удалена.");
            return;
        }

        // Если ввели: .gps x z
        if (args.length == 2) {
            try {
                targetX = Integer.parseInt(args[0]);
                targetZ = Integer.parseInt(args[1]);
                active = true;

                int dist = calculateDistance(mc.player.getX(), mc.player.getZ(), targetX, targetZ);
                sendMessage("§a[GPS] Установлена метка: §f" + targetX + ", " + targetZ + " §7(" + dist + "м)");

            } catch (NumberFormatException e) {
                sendMessage("§c[ОШИБКА] Введи числа! Пример: .gps 1000 2000");
            }
            return;
        }

        sendMessage("§cИспользование: .gps <x> <z>  или  .gps clear");
    }

    // ═══════════════════════════════════════════
    //  ОТРИСОВКА МЕТКИ НА ЭКРАНЕ (2D)
    // ═══════════════════════════════════════════
    @EventHandler
    public void onRender2D(EventRender2D event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!active || mc.player == null || mc.world == null) return;

        AWTFontRenderer font = FontManager.SMALL; // Используем твой кастомный шрифт
        if (font == null) return;

        // Вычисляем дистанцию
        int dist = calculateDistance(mc.player.getX(), mc.player.getZ(), targetX, targetZ);

        // Формируем текст
        String text = dist < 5 ? "Вы на месте!" : "GPS: " + dist + "м";
        int textWidth = font.getStringWidth(text);

        // Ставим Y метки на уровень глаз игрока, чтобы она всегда была на горизонте
        // (добавляем 0.5 к X и Z, чтобы метка была ровно по центру блока)
        Vec3d targetPos = new Vec3d(targetX + 0.5, mc.player.getY() + 1.62, targetZ + 0.5);

        // Проецируем 3D координаты мира в 2D координаты монитора
        if (!WorldToScreenUtil.worldToScreen(targetPos, screenPos)) {
            return; // Если метка за спиной игрока — не рисуем
        }

        float x = screenPos[0];
        float y = screenPos[1];
        DrawContext ctx = event.getDrawContext();

        // Отступы и размеры плашки
        int padX = 4;
        int padY = 2;
        float rectX = x - (textWidth / 2f) - padX;
        float rectY = y - font.getHeight() - padY - 5; // Сдвигаем чуть выше центра
        float rectW = textWidth + padX * 2;
        float rectH = font.getHeight() + padY * 2;

        // Рисуем красивую полупрозрачную подложку
        ctx.fill((int) rectX, (int) rectY, (int) (rectX + rectW), (int) (rectY + rectH), 0x90000000); // Темный фон
        ctx.fill((int) rectX, (int) rectY, (int) (rectX + rectW), (int) rectY + 1, 0xFF1E90FF);       // Голубая полоска сверху

        // Рисуем текст твоим шрифтом
        int textColor = dist < 5 ? 0xFF55FF55 : 0xFFFFFFFF; // Если прилетел - зеленый, иначе белый
        font.drawStringWithShadow(ctx, text, rectX + padX, rectY + padY, textColor);

        // Рисуем маленькую точку (пиксель) ровно по центру горизонта, чтобы было удобно целиться элитрами
        ctx.fill((int) x - 1, (int) y - 1, (int) x + 1, (int) y + 1, 0xFF1E90FF);
    }

    // Вспомогательный метод для подсчета дистанции (без учета высоты Y)
    private int calculateDistance(double px, double pz, int tx, int tz) {
        double dx = px - tx;
        double dz = pz - tz;
        return (int) Math.sqrt(dx * dx + dz * dz);
    }

    // Метод для локальных сообщений в чат
    private void sendMessage(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.of(text), false);
        }
    }
}