package com.pulse.client.event.events;

import com.pulse.client.event.Event;
import net.minecraft.entity.Entity;

public class EventAttack extends Event {

    private final Entity target;

    public EventAttack(Entity target) {
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }
}
