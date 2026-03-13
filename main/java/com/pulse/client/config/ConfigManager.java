package com.pulse.client.config;

import com.google.gson.*;
import com.pulse.client.PulseClient;
import com.pulse.client.module.Module;
import com.pulse.client.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.io.*;

public class ConfigManager {

    private final File dir;
    private final Gson gson;

    public ConfigManager() {
        this.dir = new File(MinecraftClient.getInstance().runDirectory, "PulseClient/configs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // Сохранение по умолчанию (срабатывает при выходе из игры)
    public void save() {
        save("default");
    }

    // Загрузка по умолчанию (срабатывает при запуске)
    public void load() {
        load("default");
    }

    // ─────────────────────────────── КАСТОМНОЕ СОХРАНЕНИЕ ─────────────────────────────── //
    public boolean save(String configName) {
        if (!configName.endsWith(".json")) configName += ".json";
        File file = new File(dir, configName);

        try {
            JsonObject root = new JsonObject();
            for (Module module : PulseClient.getInstance().getModuleManager().getModules()) {
                JsonObject moduleJson = new JsonObject();
                moduleJson.addProperty("enabled", module.isEnabled());
                moduleJson.addProperty("keybind", module.getKeybind());

                JsonObject settingsJson = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    Object val = setting.getValue();
                    if (val instanceof Boolean) settingsJson.addProperty(setting.getName(), (Boolean) val);
                    else if (val instanceof Number) settingsJson.addProperty(setting.getName(), (Number) val);
                    else if (val instanceof String) settingsJson.addProperty(setting.getName(), (String) val);
                }

                moduleJson.add("settings", settingsJson);
                root.add(module.getName(), moduleJson);
            }

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(root, writer);
            }
            return true; // Успешно сохранено
        } catch (Exception e) {
            System.err.println("[PulseClient] Ошибка при сохранении конфига: " + configName);
            e.printStackTrace();
            return false;
        }
    }

    // ─────────────────────────────── КАСТОМНАЯ ЗАГРУЗКА ─────────────────────────────── //
    @SuppressWarnings("unchecked")
    public boolean load(String configName) {
        if (!configName.endsWith(".json")) configName += ".json";
        File file = new File(dir, configName);

        if (!file.exists()) return false; // Файл не найден

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            for (Module module : PulseClient.getInstance().getModuleManager().getModules()) {
                if (root.has(module.getName())) {
                    JsonObject moduleJson = root.getAsJsonObject(module.getName());

                    if (moduleJson.has("enabled")) {
                        boolean enabled = moduleJson.get("enabled").getAsBoolean();
                        if (module.isEnabled() != enabled) module.toggle();
                    }

                    if (moduleJson.has("keybind")) {
                        module.setKeybind(moduleJson.get("keybind").getAsInt());
                    }

                    if (moduleJson.has("settings")) {
                        JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                        for (Setting<?> setting : module.getSettings()) {
                            if (settingsJson.has(setting.getName())) {
                                JsonElement element = settingsJson.get(setting.getName());
                                Object currentVal = setting.getValue();
                                try {
                                    if (currentVal instanceof Boolean) ((Setting<Boolean>) setting).setValue(element.getAsBoolean());
                                    else if (currentVal instanceof Double) ((Setting<Double>) setting).setValue(element.getAsDouble());
                                    else if (currentVal instanceof Float) ((Setting<Float>) setting).setValue(element.getAsFloat());
                                    else if (currentVal instanceof Integer) ((Setting<Integer>) setting).setValue(element.getAsInt());
                                    else if (currentVal instanceof String) ((Setting<String>) setting).setValue(element.getAsString());
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
            return true; // Успешно загружено
        } catch (Exception e) {
            System.err.println("[PulseClient] Ошибка при загрузке конфига: " + configName);
            e.printStackTrace();
            return false;
        }
    }
}