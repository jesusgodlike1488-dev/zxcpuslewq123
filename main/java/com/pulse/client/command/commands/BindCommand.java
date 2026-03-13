package com.pulse.client.command.commands;

import com.pulse.client.PulseClient;
import com.pulse.client.command.Command;
import com.pulse.client.command.CommandManager;
import com.pulse.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class BindCommand extends Command {

    public BindCommand() {
        super("bind", ".bind <module> <key>", "Binds a key to a module");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            CommandManager.sendFeedback("§cUsage: §f" + getUsage());
            return;
        }

        Module module = PulseClient.getInstance().getModuleManager().getModule(args[0]);
        if (module == null) {
            CommandManager.sendFeedback("§cModule not found: §f" + args[0]);
            return;
        }

        int key = GLFW.glfwGetKeyScancode(args[1].toUpperCase().charAt(0));
        if (key == -1) {
            try {
                key = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                CommandManager.sendFeedback("§cInvalid key: §f" + args[1]);
                return;
            }
        }

        module.setKeybind(key);
        CommandManager.sendFeedback("§7[§bPulse§7] §f" + module.getName() + " §7bound to §f" + args[1].toUpperCase());
    }
}
