package net.daporkchop.toobeetooteebot.commands.Commands;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import net.daporkchop.toobeetooteebot.Bot;
import net.daporkchop.toobeetooteebot.commands.Command;

import java.util.ArrayList;

public class ChatCommand extends Command {

    public ChatCommand() {
        super("Chat", new String[] {"chat", "c"});
    }

    @Override
    public void onExec(ArrayList<String> args) {

        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg);
        }

        Bot.getInstance().getClient().getSession().send(new ClientChatPacket(sb.toString()));
    }
}
