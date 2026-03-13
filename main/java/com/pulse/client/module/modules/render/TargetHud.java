package com.pulse.client.module.modules.render;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventRender2D;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.module.modules.combat.KillAura;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class TargetHud extends Module {


    private float x = 350;
    private float y = 250;
    private final float WIDTH  = 140;
    private final float HEIGHT = 45;

    private float animatedHealth  = 0f;  // 0..1
    private float animation       = 0f;  // 0..1 прозрачность HUD
    private float trackedMaxHealth = 0f; // максимально наблюдаемое HP цели

    private LivingEntity lastTarget = null;

    private boolean dragging = false;
    private float dragOffX   = 0f;
    private float dragOffY   = 0f;

    public TargetHud() {
        super("TargetHUD", "Показывает инфу о цели", Category.RENDER);
    }

    @Override
    public void onDisable() {
        resetState();
    }

    private void resetState() {
        lastTarget       = null;
        animation        = 0f;
        animatedHealth   = 0f;
        trackedMaxHealth = 0f;
        dragging         = false;
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null || mc.world == null) return;

        DrawContext context = e.getDrawContext();
        boolean inChat = mc.currentScreen instanceof ChatScreen;

        // ── 1. Текущая цель (свежий объект из мира по ID) ───────────────────
        LivingEntity currentTarget = null;
        if (inChat) {
            currentTarget = mc.player;
        } else if (KillAura.currentTarget instanceof LivingEntity le) {
            if (mc.world.getEntityById(le.getId()) instanceof LivingEntity fresh && fresh.isAlive()) {
                currentTarget = fresh;
            }
        }

        final float SPEED = 0.15f;

        // ── 2. Анимация появления / исчезновения ────────────────────────────
        if (currentTarget != null) {
            if (lastTarget != currentTarget) {
                // Новая цель — сбрасываем trackedMaxHealth и animatedHealth мгновенно
                trackedMaxHealth = 0f;
                float hp    = getHealth(currentTarget);
                float maxHp = getMaxHealth(currentTarget);
                animatedHealth = hp / maxHp;
                lastTarget = currentTarget;
            }
            animation += (1f - animation) * SPEED;
        } else {
            animatedHealth += (0f - animatedHealth) * SPEED;
            animation      += (0f - animation)      * SPEED;
            if (animation <= 0.01f) {
                resetState();
                return;
            }
        }

        if (animation <= 0.01f || lastTarget == null) return;

        // ── 3. Drag ─────────────────────────────────────────────────────────
        if (inChat) handleDragging();
        else dragging = false;

        // ── 4. Читаем HP ────────────────────────────────────────────────────
        LivingEntity display = (currentTarget != null) ? currentTarget : lastTarget;

        float health    = getHealth(display);
        float maxHealth = getMaxHealth(display);
        float hpRatio   = MathHelper.clamp(health / maxHealth, 0f, 1f);

        if (currentTarget != null) {
            animatedHealth += (hpRatio - animatedHealth) * SPEED;
        }

        // ── 5. Отрисовка ────────────────────────────────────────────────────
        int alpha     = (int) (180 * animation);
        int textAlpha = (int) (255 * animation);
        int intX = (int) x;
        int intY = (int) y;

        // Фон
        context.fill(intX, intY, intX + (int) WIDTH, intY + (int) HEIGHT,
                color(alpha, 20, 20, 20));

        // Аватар
        if (display instanceof AbstractClientPlayerEntity player) {
            PlayerSkinDrawer.draw(context, player.getSkinTextures(), intX + 5, intY + 6, 32);
            if (animation < 1.0f) {
                int fadeAlpha = (int) (255 * (1f - animation));
                context.fill(intX + 5, intY + 6, intX + 37, intY + 38,
                        color(fadeAlpha, 20, 20, 20));
            }
        } else {
            context.fill(intX + 5, intY + 6, intX + 37, intY + 38,
                    color(alpha, 60, 60, 60));
        }

        // Имя
        int textColor = (textAlpha << 24) | 0xFFFFFF;
        context.drawTextWithShadow(mc.textRenderer,
                display.getName().getString(), intX + 42, intY + 8, textColor);

        // HP текст
        int hpColor = getHealthColor(hpRatio, textAlpha);
        String hpText = (health == (int) health)
                ? String.format("%d HP", (int) health)
                : String.format("%.1f HP", health);
        context.drawTextWithShadow(mc.textRenderer, hpText, intX + 42, intY + 18, hpColor);

        // Полоска
        int barWidth         = (int) WIDTH - 47;
        int animatedBarWidth = (int) (barWidth * MathHelper.clamp(animatedHealth, 0f, 1f));

        context.fill(intX + 42, intY + 30, intX + 42 + barWidth, intY + 35,
                color(alpha, 40, 40, 40));
        if (animatedBarWidth > 0) {
            context.fill(intX + 42, intY + 30, intX + 42 + animatedBarWidth, intY + 35,
                    hpColor);
        }
    }

// ── Чтение HP ─────────────────────────────────────────────────────────────

    private float getHealth(LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            float scoreboardHp = getScoreboardHealth(player);
            if (scoreboardHp > 0) {
                return scoreboardHp;
            }
        }
        return entity.getHealth();
    }

    /**
     * Пытается получить HP из скорборда (слот BELOW_NAME).
     * Возвращает -1 если недоступно.
     */
    private float getScoreboardHealth(PlayerEntity player) {
        try {
            if (mc.world == null) return -1f;

            Scoreboard scoreboard = mc.world.getScoreboard();
            if (scoreboard == null) return -1f;

            // Получаем objective из слота BELOW_NAME
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            if (objective == null) return -1f;

            // Получаем счёт игрока
            ReadableScoreboardScore score = scoreboard.getScore(player, objective);
            if (score == null) return -1f;

            // Форматируем в текст
            MutableText text = ReadableScoreboardScore.getFormattedScore(
                    score, objective.getNumberFormatOr(StyledNumberFormat.EMPTY));
            String raw = text.getString();

            // Парсим число: "❤18.5" → "18.5", "§c20§r" → "20"
            String digits = raw.replaceAll("[^0-9.]", "");
            if (digits.isEmpty()) return -1f;

            // Убираем лишние точки: "1.8.5" → "1.85"
            int firstDot = digits.indexOf('.');
            if (firstDot >= 0) {
                digits = digits.substring(0, firstDot + 1)
                        + digits.substring(firstDot + 1).replace(".", "");
            }

            float hp = Float.parseFloat(digits);
            return hp > 0 ? hp : -1f;

        } catch (Exception e) {
            return -1f;
        }
    }

    /**
     * Получает максимальное HP с трекингом.
     */
    private float getMaxHealth(LivingEntity entity) {
        float attrMax = (float) entity.getAttributeValue(EntityAttributes.MAX_HEALTH);
        float currentHp = getHealth(entity);

        // Запоминаем максимальное наблюдаемое значение
        trackedMaxHealth = Math.max(trackedMaxHealth, Math.max(attrMax, currentHp));

        return Math.max(trackedMaxHealth, 1f);
    }

// ── Drag ──────────────────────────────────────────────────────────────────

    private void handleDragging() {
        double mouseX = mc.mouse.getX()
                * mc.getWindow().getScaledWidth()  / (double) mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY()
                * mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();

        boolean pressed = GLFW.glfwGetMouseButton(
                mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (pressed) {
            if (!dragging
                    && mouseX >= x && mouseX <= x + WIDTH
                    && mouseY >= y && mouseY <= y + HEIGHT) {
                dragging = true;
                dragOffX = (float) mouseX - x;
                dragOffY = (float) mouseY - y;
            }
        } else {
            dragging = false;
        }

        if (dragging) {
            x = MathHelper.clamp((float) mouseX - dragOffX, 0,
                    mc.getWindow().getScaledWidth()  - WIDTH);
            y = MathHelper.clamp((float) mouseY - dragOffY, 0,
                    mc.getWindow().getScaledHeight() - HEIGHT);
        }
    }

// ── Хелперы ───────────────────────────────────────────────────────────────

    private static int color(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static int getHealthColor(float ratio, int alpha) {
        ratio = MathHelper.clamp(ratio, 0f, 1f);
        int r = (int) (255 * (1f - ratio));
        int g = (int) (255 * ratio);
        return color(alpha, r, g, 0);
    }
}