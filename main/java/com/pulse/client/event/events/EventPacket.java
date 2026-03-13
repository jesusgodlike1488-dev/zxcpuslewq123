package com.pulse.client.event.events;

import com.pulse.client.event.Event;
import net.minecraft.network.packet.Packet;

public class EventPacket extends Event {

    public enum Direction { SEND, RECEIVE }

    private final Packet<?> packet;
    private final Direction direction;

    public EventPacket(Packet<?> packet, Direction direction) {
        this.packet = packet;
        this.direction = direction;
    }

    public Packet<?> getPacket() { return packet; }
    public Direction getDirection() { return direction; }
}
