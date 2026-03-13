package com.pulse.client.event.events;

import com.pulse.client.event.Event;

public class EventChat extends Event {

    private String message;

    public EventChat(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
