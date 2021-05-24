package net.daporkchop.toobeetooteebot.discord;

import lombok.SneakyThrows;
import net.daporkchop.toobeetooteebot.util.Constants;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordBot {

    public JDA jda;
    public MessageListener messageListener = new MessageListener();

    @SneakyThrows
    public void init() {
        try {
            final JDABuilder jdaBuilder = JDABuilder.create(Constants.CONFIG.client.extra.discordBot.login.discordToken, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES);
            Constants.DISCORD_BOT_LOG.info("Starting discord bot...");
            jda = jdaBuilder.addEventListeners(messageListener)
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOTE, CacheFlag.CLIENT_STATUS)
                    .setActivity(Activity.playing("with yer mom!1!"))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void chat(final String message) {
        if (Constants.CONFIG.client.extra.discordBot.messageSendingBehaviour.onChat) {
            processMessage(message);
        }
    }

    public void kick(final String message) {
        if (Constants.CONFIG.client.extra.discordBot.messageSendingBehaviour.onKick) {
            processMessage(message);
        }
    }

    public void onDeath(final String message) {
        if (Constants.CONFIG.client.extra.discordBot.messageSendingBehaviour.onDeath) {
            processMessage(message);
        }
    }

    public void onJoin(final String message) {
        if (Constants.CONFIG.client.extra.discordBot.messageSendingBehaviour.onLogin) {
            processMessage(message);
        }
    }

    public void connectingIn(final String message) {
        if (Constants.CONFIG.client.extra.discordBot.messageSendingBehaviour.reconnectTimer) {
            processMessage(message);
        }
    }

    private void processMessage(final String message) {
        try {
            for (String s : Constants.CONFIG.client.extra.discordBot.messageSendingBehaviour.guildChannelsToSendLog) {
                jda.getTextChannelById(s).sendMessage(message).queue();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
