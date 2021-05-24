package net.daporkchop.toobeetooteebot.commands.Commands;

import net.daporkchop.toobeetooteebot.Bot;
import net.daporkchop.toobeetooteebot.commands.Command;

import java.util.ArrayList;

public class ExitCommand extends Command {

    public ExitCommand() {
        super("Exit", new String[]{"exit", "shutdown"});
    }

    @Override
    public void onExec(ArrayList<String> args) {
        Bot.getInstance().shuttingDown = true;
    }
}
