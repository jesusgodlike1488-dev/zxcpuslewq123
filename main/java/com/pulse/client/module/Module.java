package com.pulse.client.module;

import com.pulse.client.PulseClient;
import com.pulse.client.event.IListener;
import com.pulse.client.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module implements IListener {

    protected final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private int keybind;
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = false;
        this.keybind = -1;
    }

    public Module(String name, String description, Category category, int keybind) {
        this(name, description, category);
        this.keybind = keybind;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            onEnable();
            PulseClient.getInstance().getEventBus().register(this);
        } else {
            onDisable();
            PulseClient.getInstance().getEventBus().unregister(this);
        }
        // Notify ModuleManager to rebuild enabled cache
        PulseClient.getInstance().getModuleManager().notifyToggle();
    }

    public void onEnable() {}

    public void onDisable() {}

    protected <T> Setting<T> register(Setting<T> setting) {
        settings.add(setting);
        return setting;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKeybind() { return keybind; }
    public void setKeybind(int keybind) { this.keybind = keybind; }
    public List<Setting<?>> getSettings() { return settings; }
}
