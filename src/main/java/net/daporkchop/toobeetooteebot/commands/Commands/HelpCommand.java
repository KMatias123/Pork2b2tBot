package net.daporkchop.toobeetooteebot.commands.Commands;

import net.daporkchop.toobeetooteebot.Bot;
import net.daporkchop.toobeetooteebot.commands.Command;
import net.daporkchop.toobeetooteebot.util.Constants;

import java.util.ArrayList;

public class HelpCommand extends Command {

    @Override
    public void onExec(ArrayList<String> args) {
        StringBuilder sb = new StringBuilder();
        sb.append("Available commands: ");
        for (Command c : Bot.getInstance().getCommandManager().getCommands()) {
            sb.append(c.getName()).append(" ");
        }
        Constants.DEFAULT_LOG.info(sb.toString());
    }

    public HelpCommand() {
        super("Help", new String[]{"help", "commands"});
    }
}
