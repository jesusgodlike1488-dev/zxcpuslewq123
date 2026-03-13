package com.pulse.client.command.commands;

import com.pulse.client.PulseClient;
import com.pulse.client.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        // Название команды, описание, алиасы (сокращения)
        super("config", ".cfg save [имя]", "Сохраняет и загружает конфиги");
    }

    @Override
    public void execute(String[] args) {
        // Проверяем, что игрок ввел достаточно аргументов: .cfg save myconfig
        if (args.length < 2) {
            sendMsg("§cИспользование: .cfg <save/load> <имя>");
            return;
        }

        String action = args[0].toLowerCase();
        String configName = args[1];

        if (action.equals("save")) {
            boolean success = PulseClient.getInstance().getConfigManager().save(configName);
            if (success) {
                sendMsg("§aУспешно сохранен конфиг: §f" + configName);
            } else {
                sendMsg("§cНе удалось сохранить конфиг: §f" + configName);
            }

        } else if (action.equals("load")) {
            boolean success = PulseClient.getInstance().getConfigManager().load(configName);
            if (success) {
                sendMsg("§aУспешно загружен конфиг: §f" + configName);
            } else {
                sendMsg("§cКонфиг §f" + configName + " §cне найден!");
            }

        } else {
            sendMsg("§cНеизвестное действие! Используй save или load.");
        }
    }

    // Хелпер для отправки сообщений в чат (только для тебя, на сервер не отправляется)
    private void sendMsg(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§7[§bPulse§7] " + message), false);
        }
    }
}