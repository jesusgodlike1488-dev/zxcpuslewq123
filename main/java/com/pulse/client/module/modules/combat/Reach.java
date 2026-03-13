package com.pulse.client.module.modules.combat;

import com.pulse.client.module.Category;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;

public class Reach extends Module {

    public final Setting<Double> reach = register(new Setting<Double>("Reach", 5.0, "Crosshair entity range").setRange(3.0, 8.0));

    public Reach() {
        super("Reach", "Extends crosshair entity interaction range", Category.COMBAT);
    }
}
