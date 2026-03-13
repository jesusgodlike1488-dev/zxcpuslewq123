package com.pulse.client.command;

import com.pulse.client.command.commands.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {

    private final List<Command> commands = new ArrayList<>();

    public CommandManager() {
        register(new ToggleCommand());
        register(new BindCommand());
        register(new HelpCommand());
        register(new GpsCommand());
        register(new ConfigCommand());
    }

    private void register(Command command) {
        commands.add(command);
    }

    public void dispatch(String input) {
        String[] parts = input.trim().split(" ");
        if (parts.length == 0) return;

        String name = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(name)) {
                command.execute(args);
                return;
            }
        }

        sendFeedback("§cUnknown command: §f." + name + " §c— try §f.help");
    }

    public static void sendFeedback(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message), false);
        }
    }

    public List<Command> getCommands() {
        return commands;
    }
}
