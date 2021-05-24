package net.daporkchop.toobeetooteebot.commands;

import lombok.Getter;

import java.util.ArrayList;

public class Command {

    @Getter
    private final String name;
    @Getter
    private final String[] aliases;


    public Command(String name, String[] aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    public void onExec(ArrayList<String> args) {

    }
}
