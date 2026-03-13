package com.pulse.client.command.commands;

import com.pulse.client.PulseClient;
import com.pulse.client.command.Command;
import com.pulse.client.command.CommandManager;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", ".help", "Lists all commands");
    }

    @Override
    public void execute(String[] args) {
        CommandManager.sendFeedback("§7[§bPulse§7] §fCommands:");
        for (Command cmd : PulseClient.getInstance().getCommandManager().getCommands()) {
            CommandManager.sendFeedback("  §b" + cmd.getUsage() + " §7— " + cmd.getDescription());
        }
    }
}
