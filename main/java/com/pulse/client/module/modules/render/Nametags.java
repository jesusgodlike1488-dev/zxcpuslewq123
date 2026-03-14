package com.pulse.client.module.modules.render;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventRender2D;
import com.pulse.client.gui.font.AWTFontRenderer;
import com.pulse.client.gui.font.FontManager;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import com.pulse.client.util.WorldToScreenUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardScore;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Оптимизированные Nametags для 1.20.1.
 * Без выделения лишней памяти в цикле рендера (GC-friendly).
 */
public class Nametags extends Module {

    public final Setting<Boolean> showHealth  = register(new Setting<>("Health",  true, "Показывать здоровье"));
    public final Setting<Boolean> showArmor   = register(new Setting<>("Armor",   true, "Показывать броню"));
    public final Setting<Boolean> showEffects = register(new Setting<>("Effects", true, "Показывать эффекты"));
    public final Setting<Boolean> showItem    = register(new Setting<>("HeldItem", true, "Показывать предмет в руке"));
    public final Setting<Double>  scale       = register(new Setting<>("Scale",   0.6, "Размер неймтега").setRange(0.3, 2.0));

    private final float[] screenPos = new float[2];

    // Список для правильной сортировки (чтобы ближние перекрывали дальних)
    private final List<PlayerEntity> sortedPlayers = new ArrayList<>();

    // Layout
    private static final int PAD_X = 5;
    private static final int PAD_Y = 3;
    private static final int ROW_GAP = 2;
    private static final int BAR_HEIGHT = 2;
    private static final int ITEM_ICON = 16;
    private static final int ICON_GAP = 2;

    // Слоты брони (голова → ботинки)
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    // Цвета
    private static final int C_BG       = 0xB0080810;
    private static final int C_ACCENT   = 0xFF1E90FF;
    private static final int C_NAME     = 0xFFFFFFFF;
    private static final int C_ITEM     = 0xFF999999;
    private static final int C_BAR_BG   = 0xFF1A1A22;
    private static final int C_BUFF     = 0xFF77BBEE;
    private static final int C_DEBUFF   = 0xFFEE6666;

    // Кеш для эффектов (O(1) поиск)
    private static final Map<StatusEffect, String> EFFECT_NAMES = new HashMap<>();
    private static final Map<StatusEffect, Integer> EFFECT_COLORS = new HashMap<>();

    static {
        // Баффы
        addEffect(StatusEffects.SPEED, "Spd", C_BUFF);
        addEffect(StatusEffects.HASTE, "Hst", C_BUFF);
        addEffect(StatusEffects.STRENGTH, "Str", C_BUFF);
        addEffect(StatusEffects.INSTANT_HEALTH, "Heal", C_BUFF);
        addEffect(StatusEffects.JUMP_BOOST, "Jmp", C_BUFF);
        addEffect(StatusEffects.REGENERATION, "Reg", C_BUFF);
        addEffect(StatusEffects.RESISTANCE, "Res", C_BUFF);
        addEffect(StatusEffects.FIRE_RESISTANCE, "FRes", C_BUFF);
        addEffect(StatusEffects.WATER_BREATHING, "WBr", C_BUFF);
        addEffect(StatusEffects.INVISIBILITY, "Inv", C_BUFF);
        addEffect(StatusEffects.NIGHT_VISION, "NV", C_BUFF);
        addEffect(StatusEffects.ABSORPTION, "Abs", C_BUFF);
        addEffect(StatusEffects.SATURATION, "Sat", C_BUFF);
        addEffect(StatusEffects.SLOW_FALLING, "SF", C_BUFF);

        // Дебаффы
        addEffect(StatusEffects.SLOWNESS, "Slow", C_DEBUFF);
        addEffect(StatusEffects.MINING_FATIGUE, "Ftg", C_DEBUFF);
        addEffect(StatusEffects.INSTANT_DAMAGE, "Dmg", C_DEBUFF);
        addEffect(StatusEffects.NAUSEA, "Nau", C_DEBUFF);
        addEffect(StatusEffects.BLINDNESS, "Bld", C_DEBUFF);
        addEffect(StatusEffects.HUNGER, "Hgr", C_DEBUFF);
        addEffect(StatusEffects.WEAKNESS, "Wk", C_DEBUFF);
        addEffect(StatusEffects.POISON, "Poi", C_DEBUFF);
        addEffect(StatusEffects.WITHER, "Wth", C_DEBUFF);
        addEffect(StatusEffects.GLOWING, "Glo", C_DEBUFF);
        addEffect(StatusEffects.LEVITATION, "Lev", C_DEBUFF);
    }

    private static void addEffect(StatusEffect effect, String name, int color) {
        EFFECT_NAMES.put(effect, name);
        EFFECT_COLORS.put(effect, color);
    }

    public Nametags() {
        super("Nametags", "Неймтеги с бронёй, эффектами и предметом в руке", Category.RENDER);
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (mc.world == null || mc.player == null || mc.gameRenderer == null) return;

        AWTFontRenderer fontSmall = FontManager.SMALL;
        if (fontSmall == null) return;

        // Собираем игроков
        sortedPlayers.clear();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            if (mc.player.squaredDistanceTo(player) > 4096) continue; // 64 блока
            sortedPlayers.add(player);
        }

        if (sortedPlayers.isEmpty()) return;

        // Сортировка по дистанции (дальние рендерятся первыми, ближние - поверх них)
        sortedPlayers.sort((p1, p2) -> Double.compare(
                mc.player.squaredDistanceTo(p2),
                mc.player.squaredDistanceTo(p1)
        ));

        DrawContext ctx = event.getDrawContext();
        float tickDelta = event.getTickDelta();

        for (PlayerEntity player : sortedPlayers) {
            renderNametag(ctx, player, tickDelta, fontSmall);
        }
    }

    private void renderNametag(DrawContext ctx, PlayerEntity player, float tickDelta, AWTFontRenderer font) {
        Vec3d pos = player.getLerpedPos(tickDelta).add(0, player.getHeight() + 0.3, 0);

        if (!WorldToScreenUtil.worldToScreen(pos, screenPos)) return;

        float centerX = screenPos[0];
        float baseY   = screenPos[1];
        float sc      = scale.getValue().floatValue();

        String name = player.getName().getString();
        int nameW = font.getStringWidth(name);

        // Здоровье
        float health = getHealth(player);
        float maxHealth = getMaxHealth(player);
        float hpRatio = MathHelper.clamp(health / Math.max(maxHealth, 1f), 0f, 1f);

        String hpText = "";
        int hpW = 0;
        if (showHealth.getValue()) {
            hpText = formatHp(health);
            hpW = font.getStringWidth(hpText);
        }

        int gapW = hpW > 0 ? font.getStringWidth("  ") : 0;
        int line1W = nameW + gapW + hpW;

        // Броня
        int armorCount = 0;
        if (showArmor.getValue()) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                if (!player.getEquippedStack(slot).isEmpty()) armorCount++;
            }
        }
        int armorRowW = armorCount == 0 ? 0 : armorCount * ITEM_ICON + (armorCount - 1) * ICON_GAP;

        // Эффекты
        int effectsW = 0;
        if (showEffects.getValue()) {
            for (StatusEffectInstance effect : player.getStatusEffects()) {
                String text = buildEffectText(effect);
                if (effectsW > 0) effectsW += font.getStringWidth(" ");
                effectsW += font.getStringWidth(text);
            }
        }

        // Предмет
        String itemName = "";
        int itemW = 0;
        if (showItem.getValue()) {
            ItemStack mainHand = player.getMainHandStack();
            if (!mainHand.isEmpty()) {
                itemName = mainHand.getName().getString();
                if (mainHand.getCount() > 1) itemName += " x" + mainHand.getCount();
                itemW = font.getStringWidth(itemName);
            }
        }

        // Габариты панели
        int contentW = line1W;
        if (showHealth.getValue()) contentW = Math.max(contentW, 50);
        if (armorRowW > 0) contentW = Math.max(contentW, armorRowW);
        if (effectsW > 0) contentW = Math.max(contentW, effectsW);
        if (itemW > 0) contentW = Math.max(contentW, itemW);

        int panelW = contentW + PAD_X * 2;

        int contentH = font.getHeight();
        if (showHealth.getValue()) contentH += ROW_GAP + BAR_HEIGHT;
        if (armorCount > 0) contentH += ROW_GAP + ITEM_ICON;
        if (effectsW > 0) contentH += ROW_GAP + font.getHeight();
        if (itemW > 0) contentH += ROW_GAP + font.getHeight();

        int panelH = contentH + PAD_Y * 2;

        // Отрисовка
        ctx.getMatrices().push();
        ctx.getMatrices().scale(sc, sc, 1f);

        float adjCX = centerX / sc;
        float adjBY = baseY / sc;

        float panelX = adjCX - panelW / 2f;
        float panelY = adjBY - panelH - 2;

        int ix = (int) panelX;
        int iy = (int) panelY;

        ctx.fill(ix, iy, ix + panelW, iy + panelH, C_BG);
        ctx.fill(ix, iy, ix + panelW, iy + 1, C_ACCENT);

        float drawY = panelY + PAD_Y;
        float ccx = panelX + panelW / 2f;

        float lineX = ccx - line1W / 2f;
        font.drawStringWithShadow(ctx, name, lineX, drawY, C_NAME);
        lineX += nameW;

        if (hpW > 0) {
            lineX += gapW;
            font.drawStringWithShadow(ctx, hpText, lineX, drawY, getHealthColor(hpRatio));
        }
        drawY += font.getHeight();

        if (showHealth.getValue()) {
            drawY += ROW_GAP;
            int barW = panelW - PAD_X * 2;
            float barX = panelX + PAD_X;

            ctx.fill((int) barX, (int) drawY, (int) barX + barW, (int) drawY + BAR_HEIGHT, C_BAR_BG);
            int filledW = (int) (barW * hpRatio);
            if (filledW > 0) {
                ctx.fill((int) barX, (int) drawY, (int) barX + filledW, (int) drawY + BAR_HEIGHT, getHealthColor(hpRatio));
            }
            drawY += BAR_HEIGHT;
        }

        if (armorCount > 0) {
            drawY += ROW_GAP;
            float armorX = ccx - armorRowW / 2f;

            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = player.getEquippedStack(slot);
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, (int) armorX, (int) drawY);
                    armorX += ITEM_ICON + ICON_GAP;
                }
            }
            drawY += ITEM_ICON;
        }

        if (effectsW > 0) {
            drawY += ROW_GAP;
            float effX = ccx - effectsW / 2f;

            for (StatusEffectInstance effect : player.getStatusEffects()) {
                String text = buildEffectText(effect);
                font.drawString(ctx, text, effX, drawY, getEffectColor(effect));
                effX += font.getStringWidth(text) + font.getStringWidth(" ");
            }
            drawY += font.getHeight();
        }

        if (itemW > 0) {
            drawY += ROW_GAP;
            font.drawCenteredString(ctx, itemName, ccx, drawY, C_ITEM);
        }

        ctx.getMatrices().pop();
    }

    private float getHealth(PlayerEntity player) {
        float sbHp = getScoreboardHealth(player);
        return sbHp > 0 ? sbHp : player.getHealth() + player.getAbsorptionAmount();
    }

    private float getMaxHealth(PlayerEntity player) {
        float attrMax = (float) player.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        return Math.max(attrMax, Math.max(getHealth(player), 1f));
    }

    private float getScoreboardHealth(PlayerEntity player) {
        try {
            if (mc.world == null) return -1f;
            Scoreboard sb = mc.world.getScoreboard();

            // В 1.20.4 используется Enum вместо цифр
            ScoreboardObjective obj = sb.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.BELOW_NAME);
            if (obj == null) return -1f;

            // В 1.20.4 игрок сам является ScoreHolder'ом, имя получать не нужно
            net.minecraft.scoreboard.ReadableScoreboardScore score = sb.getScore(player, obj);
            if (score == null) return -1f;

            return score.getScore();
        } catch (Exception e) {
            return -1f;
        }
    }

    private String formatHp(float hp) {
        if (hp == (int) hp) return (int) hp + "HP";
        return String.format("%.1fHP", hp).replace(",", ".");
    }

    private static int getHealthColor(float ratio) {
        ratio = MathHelper.clamp(ratio, 0f, 1f);
        int r, g;
        if (ratio < 0.5f) {
            r = 255;
            g = (int) (255 * (ratio / 0.5f));
        } else {
            r = (int) (255 * ((1f - ratio) / 0.5f));
            g = 255;
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    private String buildEffectText(StatusEffectInstance effect) {
        String n = EFFECT_NAMES.getOrDefault(effect.getEffectType(), getFallbackEffectName(effect));
        int amp = effect.getAmplifier();
        if (amp > 0) n += toRoman(amp + 1);

        int ticks = effect.getDuration();
        if (ticks > 0 && ticks < 999999) {
            int sec = ticks / 20;
            int m = sec / 60;
            int s = sec % 60;
            n += " " + (m > 0 ? m + ":" + String.format("%02d", s) : s + "s");
        }
        return n;
    }

    private int getEffectColor(StatusEffectInstance effect) {
        return EFFECT_COLORS.getOrDefault(effect.getEffectType(), 0xFF999999);
    }

    private String getFallbackEffectName(StatusEffectInstance effect) {
        try {
            Identifier id = Registries.STATUS_EFFECT.getId(effect.getEffectType());
            if (id == null) return "?";
            String path = id.getPath();
            return path.length() > 4 ? path.substring(0, 4) : path;
        } catch (Exception e) { return "?"; }
    }

    private String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III";
            case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(n);
        };
    }
}