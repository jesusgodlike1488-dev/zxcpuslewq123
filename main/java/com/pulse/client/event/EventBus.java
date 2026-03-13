package com.pulse.client.event;

import com.pulse.client.PulseClient;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Optimized EventBus: uses MethodHandle instead of Method.invoke()
 * MethodHandle is ~5x faster after warmup because HotSpot can inline it.
 */
public class EventBus {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private final Map<Class<?>, List<ListenerEntry>> listenerMap = new ConcurrentHashMap<>();

    public void register(IListener listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventHandler.class)) continue;
            if (method.getParameterCount() != 1) continue;

            Class<?> eventClass = method.getParameterTypes()[0];
            method.setAccessible(true);

            try {
                MethodHandle handle = LOOKUP.unreflect(method);
                listenerMap.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                        .add(new ListenerEntry(listener, handle));
            } catch (IllegalAccessException e) {
                PulseClient.LOGGER.error("Failed to create MethodHandle for {}", method.getName(), e);
            }
        }
    }

    public void unregister(IListener listener) {
        for (List<ListenerEntry> entries : listenerMap.values()) {
            entries.removeIf(e -> e.listener == listener);
        }
    }

    public void post(Object event) {
        List<ListenerEntry> entries = listenerMap.get(event.getClass());
        if (entries == null || entries.isEmpty()) return;

        for (int i = 0, size = entries.size(); i < size; i++) {
            ListenerEntry entry = entries.get(i);
            try {
                entry.handle.invoke(entry.listener, event);
            } catch (Throwable e) {
                PulseClient.LOGGER.error("EventBus dispatch error", e);
            }
        }
    }

    private static final class ListenerEntry {
        final IListener listener;
        final MethodHandle handle;

        ListenerEntry(IListener listener, MethodHandle handle) {
            this.listener = listener;
            this.handle = handle;
        }
    }
}
