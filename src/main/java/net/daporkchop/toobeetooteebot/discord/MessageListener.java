package net.daporkchop.toobeetooteebot.discord;

import net.daporkchop.toobeetooteebot.Bot;
import net.daporkchop.toobeetooteebot.util.Constants;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (Constants.CONFIG.client.extra.discordBot.discordCommands.enabled &&
            event.getMessage().getContentRaw().startsWith(Constants.CONFIG.client.extra.discordBot.prefix)) {
            if (Constants.CONFIG.client.extra.discordBot.discordCommands.whiteList.enabled) {
                if (Constants.CONFIG.client.extra.discordBot.discordCommands.whiteList.whitelist.contains(event.getMessageId())) {
                    executeCommand(event.getMessage().getContentRaw());
                    Constants.DISCORD_BOT_COMMAND.info("Executed command: " + event.getMessage().getContentRaw());
                }
            } else {
                executeCommand(event.getMessage().getContentRaw());
                Constants.DISCORD_BOT_COMMAND.info("Executed command: " + event.getMessage().getContentRaw());
            }
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        Constants.DISCORD_BOT_LOG.success("Discord bot connected!");
    }

    private void executeCommand(String command) {
        Pattern pattern = Pattern.compile(Constants.CONFIG.client.extra.discordBot.prefix, Pattern.LITERAL);
        Matcher matcher = pattern.matcher(command);
        matcher.replaceFirst("");
        Bot.getInstance().getCommandManager().executeCommand(command);
    }
}
