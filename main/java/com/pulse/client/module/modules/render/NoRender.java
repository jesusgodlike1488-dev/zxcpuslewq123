package com.pulse.client.module.modules.render;

import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;

public class NoRender extends Module {

    public final Setting<Boolean> hurtCam = register(new Setting<>("HurtCam", true, "Убирает тряску экрана при уроне"));
    public final Setting<Boolean> totemPop = register(new Setting<>("TotemPop", true, "Убирает анимацию тотема на весь экран"));

    public NoRender() {
        super("NoRender", "Убирает бесячие визуальные эффекты", Category.RENDER);
    }

    // Вся логика будет в Миксине!
}