package net.daporkchop.toobeetooteebot.commands.Commands;

import net.daporkchop.toobeetooteebot.Bot;
import net.daporkchop.toobeetooteebot.commands.Command;

import java.util.ArrayList;

public class ReconnectCommand extends Command {

    public ReconnectCommand() {
        super("Reconnect", new String[]{"reconnect"});
    }

    @Override
    public void onExec(ArrayList<String> args) {
        Bot.getInstance().getClient().getSession().disconnect("Manual disconnect");
    }
}
