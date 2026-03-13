package com.pulse.client.module.modules.render;

import com.pulse.client.event.EventHandler;
import com.pulse.client.event.events.EventUpdate;
import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright extends Module {

    public Fullbright() {
        super("Fullbright", "Makes the world fully lit", Category.RENDER);
    }

    @Override
    public void onEnable() {
        // Ничего не нужно сохранять
    }

    @EventHandler
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        // Выдаем бесконечное Ночное Зрение каждую секунду (чтобы не пропадало).
        // Аргументы: Эффект, Длительность(в тиках), Уровень, ambient, showParticles, showIcon
        // Ставим последние три false, чтобы убрать партиклы и иконку из инвентаря!
        mc.player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION,
                400,
                0,
                false,
                false,
                false
        ));
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            // Как только выключаем модуль - удаляем эффект, и мир снова становится темным
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }
}