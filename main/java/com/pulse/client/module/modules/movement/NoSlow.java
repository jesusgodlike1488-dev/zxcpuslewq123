package com.pulse.client.module.modules.movement;

import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;

public class NoSlow extends Module {

    public final Setting<Boolean> items    = register(new Setting<>("Items",    true, "Cancel item use slowdown"));
    public final Setting<Boolean> web      = register(new Setting<>("Web",      true, "Cancel cobweb slowdown"));
    public final Setting<Boolean> soulsand = register(new Setting<>("SoulSand", true, "Cancel soulsand slowdown"));

    public NoSlow() {
        super("NoSlow", "Prevents movement slowdown", Category.MOVEMENT);
    }
}
