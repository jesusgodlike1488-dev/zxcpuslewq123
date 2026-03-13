package com.pulse.client.command.commands;

import com.pulse.client.PulseClient;
import com.pulse.client.command.Command;
import com.pulse.client.command.CommandManager;
import com.pulse.client.module.Module;

public class ToggleCommand extends Command {

    public ToggleCommand() {
        super("toggle", ".toggle <module>", "Toggles a module on or off");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            CommandManager.sendFeedback("§cUsage: §f" + getUsage());
            return;
        }

        Module module = PulseClient.getInstance().getModuleManager().getModule(args[0]);
        if (module == null) {
            CommandManager.sendFeedback("§cModule not found: §f" + args[0]);
            return;
        }

        module.toggle();
        CommandManager.sendFeedback("§7[§bPulse§7] §f" + module.getName() + " §7→ " + (module.isEnabled() ? "§aON" : "§cOFF"));
    }
}
