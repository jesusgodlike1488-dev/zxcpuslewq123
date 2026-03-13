package com.pulse.client.event.events;

import com.pulse.client.event.Event;
import net.minecraft.client.gui.DrawContext;

public class EventRender2D extends Event {

    private final DrawContext drawContext;
    private final float tickDelta;

    public EventRender2D(DrawContext drawContext, float tickDelta) {
        this.drawContext = drawContext;
        this.tickDelta = tickDelta;
    }

    public DrawContext getDrawContext() {
        return drawContext;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}
