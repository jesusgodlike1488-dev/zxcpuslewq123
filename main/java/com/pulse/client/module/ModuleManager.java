package com.pulse.client.module;

import com.pulse.client.module.modules.combat.KillAura;
import com.pulse.client.module.modules.combat.Reach;
import com.pulse.client.module.modules.combat.TriggerBot;
import com.pulse.client.module.modules.movement.NoSlow;
import com.pulse.client.module.modules.movement.Sprint;
import com.pulse.client.module.modules.movement.Step;
import com.pulse.client.module.modules.player.*;
import com.pulse.client.module.modules.render.*;

import java.util.*;

/**
 * Optimized ModuleManager:
 * - HashMap for O(1) name lookups instead of O(n) stream filter
 * - HashMap for O(1) class lookups
 * - Pre-built category lists (no stream allocation per call)
 * - Cached enabled modules list (rebuilt only on toggle via notifyToggle)
 */
public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();
    private final Map<String, Module> nameMap = new HashMap<>();
    private final Map<Class<? extends Module>, Module> classMap = new HashMap<>();
    private final Map<Category, List<Module>> categoryMap = new EnumMap<>(Category.class);

    // Cached enabled list — rebuilt on toggle
    private List<Module> enabledCache = Collections.emptyList();
    private boolean enabledDirty = true;

    public ModuleManager() {
        // Pre-fill category map
        for (Category cat : Category.values()) {
            categoryMap.put(cat, new ArrayList<>());
        }

        // Movement
        register(new Sprint());
        register(new NoSlow());
        register(new Step());

        // Combat
        register(new KillAura());
        register(new Reach());
        register(new TriggerBot());

        // Render
        register(new ESP());
        register(new NoRender());
        register(new BlockESP());
        register(new ItemESP());
        register(new TargetHud());
        register(new Fullbright());
        register(new Tracers());
        register(new Nametags());

        // Player
        register(new AutoTotem());
        register(new ChestStealer());
        register(new AntiAFK());
        register(new ClickPearl());
        register(new ItemScroller());
    }

    private void register(Module module) {
        modules.add(module);
        nameMap.put(module.getName().toLowerCase(Locale.ROOT), module);
        classMap.put(module.getClass(), module);
        categoryMap.get(module.getCategory()).add(module);
    }

    /**
     * Called from Module.setEnabled() to invalidate enabled cache.
     */
    public void notifyToggle() {
        enabledDirty = true;
    }

    // O(1) lookup by name
    public Module getModule(String name) {
        return nameMap.get(name.toLowerCase(Locale.ROOT));
    }

    // O(1) lookup by class
    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        return (T) classMap.get(clazz);
    }

    public List<Module> getModules() {
        return modules;
    }

    // O(1) — pre-built list per category
    public List<Module> getModulesByCategory(Category c) {
        return categoryMap.getOrDefault(c, Collections.emptyList());
    }

    // Cached — only rebuilds when a module was toggled
    public List<Module> getEnabledModules() {
        if (enabledDirty) {
            List<Module> list = new ArrayList<>();
            for (int i = 0, size = modules.size(); i < size; i++) {
                if (modules.get(i).isEnabled()) list.add(modules.get(i));
            }
            enabledCache = list;
            enabledDirty = false;
        }
        return enabledCache;
    }
}
