package net.daporkchop.toobeetooteebot.commands;

import lombok.Getter;
import net.daporkchop.toobeetooteebot.commands.Commands.ChatCommand;
import net.daporkchop.toobeetooteebot.commands.Commands.ExitCommand;
import net.daporkchop.toobeetooteebot.commands.Commands.HelpCommand;
import net.daporkchop.toobeetooteebot.util.Constants;

import java.util.ArrayList;
import java.util.Arrays;

public class Manager {

    @Getter
    private final ArrayList<Command> commands = new ArrayList<>();

    public Command getCommandForName(final String name) {
        for (Command command : commands) {
            for (String alias : command.getAliases()) {
                if (alias.equalsIgnoreCase(name)) {
                    return command;
                }
            }
        }
        return null;
    }

    public void init() {
        commands.add(new HelpCommand());
        commands.add(new ChatCommand());
        commands.add(new ExitCommand());
    }

    public void executeCommand(final String commandString) {
        String[] args = commandString.trim().split(" ");

        final ArrayList<String> realArgs = new ArrayList<>(Arrays.asList(args));

        final Command command = getCommandForName(args[0]);

        if (command == null) {
            Constants.DEFAULT_LOG.info("This is not a command, you can get all the valid commands with the command \"help\".");
            return;
        }

        realArgs.remove(0);

        command.onExec(realArgs);
    }
}
