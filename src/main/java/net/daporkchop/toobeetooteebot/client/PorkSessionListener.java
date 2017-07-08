package net.daporkchop.toobeetooteebot.client;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.data.game.world.WorldType;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.data.message.TextMessage;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.*;
import com.github.steveice10.mc.protocol.packet.login.server.LoginDisconnectPacket;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;
import net.daporkchop.toobeetooteebot.gui.GuiBot;
import net.daporkchop.toobeetooteebot.server.PorkClient;
import net.daporkchop.toobeetooteebot.server.PorkServerAdapter;
import net.daporkchop.toobeetooteebot.util.ChunkPos;
import net.daporkchop.toobeetooteebot.util.Config;
import net.daporkchop.toobeetooteebot.util.TextFormat;
import net.daporkchop.toobeetooteebot.web.PlayData;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

import java.net.Proxy;
import java.util.Iterator;
import java.util.TimerTask;

public class PorkSessionListener implements SessionListener {
    public TooBeeTooTeeBot bot;

    public PorkSessionListener(TooBeeTooTeeBot tooBeeTooTeeBot) {
        bot = tooBeeTooTeeBot;
    }

    @Override
    public void packetReceived(PacketReceivedEvent packetReceivedEvent) {
        //System.out.println(packetReceivedEvent.getPacket().getClass().getCanonicalName());
        try {
            BREAK:
            if (true) {
                if (packetReceivedEvent.getPacket() instanceof ServerChatPacket) {
                    ServerChatPacket pck = packetReceivedEvent.getPacket();
                    String messageJson = pck.getMessage().toJsonString();
                    String legacyColorCodes = BaseComponent.toLegacyText(ComponentSerializer.parse(messageJson));
                    String msg = TextFormat.clean(legacyColorCodes);
                    if (Config.processChat) {
                        if (msg.startsWith("To ")) {
                            //don't bother processing sent DMs
                            return;
                        }
                        try {
                            String[] split = msg.split(" ");
                            if (!msg.startsWith("<") && split[1].startsWith("whispers")) {
                                bot.processMsg(split[0], msg.substring(split[0].length() + split[1].length() + 2));
                                return;
                            }
                        } catch (ArrayIndexOutOfBoundsException e) {
                            //ignore kek
                        }
                    }

                    if (msg.startsWith("!")) { //command from connected user
                        if (msg.startsWith("!toggleafk")) { //useful when manually moving bot around
                            Config.doAntiAFK = !Config.doAntiAFK;
                            System.out.println("! Toggled AntiAFK! Current state: " + (Config.doAntiAFK ? "on" : "off"));
                            bot.queueMessage("! Toggled AntiAFK! Current state: " + (Config.doAntiAFK ? "on" : "off"));
                        }
                        return;
                    }
                    System.out.println("[CHAT] " + msg);

                    if (GuiBot.INSTANCE != null) {
                        GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>" + msg + "</html>");
                        String[] split = GuiBot.INSTANCE.chatDisplay.getText().split("<br>");
                        if (split.length > 500) {
                            String toSet = "<html>";
                            for (int i = 1; i < split.length; i++) {
                                toSet += split[i] + "<br>";
                            }
                            toSet = toSet.substring(toSet.length() - 4) + "</html>";
                            GuiBot.INSTANCE.chatDisplay.setText(toSet);
                        }
                    }

                    if (Config.doDiscord) {
                        bot.queuedMessages.add(msg);
                    }
                    if (bot.websocketServer != null) {
                        bot.websocketServer.sendToAll("chat    " + legacyColorCodes.replace("<", "&lt;").replace(">", "&gt;"));
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerHealthPacket) {
                    ServerPlayerHealthPacket pck = packetReceivedEvent.getPacket();
                    if (Config.doAutoRespawn) {
                        if (pck.getHealth() < 1) {
                            bot.timer.schedule(new TimerTask() { // respawn
                                @Override
                                public void run() {
                                    bot.client.getSession().send(new ClientRequestPacket(ClientRequest.RESPAWN));
                                    bot.cachedChunks.clear(); //memory leak
                                }
                            }, 100);
                        }
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerListEntryPacket) {
                    ServerPlayerListEntryPacket pck = packetReceivedEvent.getPacket();
                    switch (pck.getAction()) {
                        case ADD_PLAYER:
                            LOOP:
                            for (PlayerListEntry entry : pck.getEntries()) {
                                for (PlayerListEntry listEntry : bot.playerListEntries) {
                                    if (listEntry.getProfile().getIdAsString().equals(entry.getProfile().getIdAsString())) {
                                        continue LOOP;
                                    }
                                }
                                bot.playerListEntries.add(entry);
                                if (bot.websocketServer != null) {
                                    bot.websocketServer.sendToAll("tabAdd  " + TooBeeTooTeeBot.getName(entry) + " " + entry.getPing() + " " + entry.getProfile().getIdAsString());
                                }
                                if (Config.doStatCollection) {
                                    String uuid = entry.getProfile().getId().toString();
                                    if (bot.uuidsToPlayData.containsKey(uuid)) {
                                        PlayData data = bot.uuidsToPlayData.get(uuid);
                                        data.lastPlayed = System.currentTimeMillis();
                                    } else {
                                        PlayData data = new PlayData(uuid, TooBeeTooTeeBot.getName(entry));
                                        bot.uuidsToPlayData.put(data.UUID, data);
                                    }
                                }
                            }
                            break;
                        case UPDATE_GAMEMODE:
                            //ignore
                            break;
                        case UPDATE_LATENCY:
                            for (PlayerListEntry entry : pck.getEntries()) {
                                String uuid = entry.getProfile().getId().toString();
                                for (PlayerListEntry toChange : bot.playerListEntries) {
                                    if (uuid.equals(toChange.getProfile().getId().toString())) {
                                        toChange.ping = entry.getPing();
                                        if (bot.websocketServer != null) {
                                            bot.websocketServer.sendToAll("tabPing " + toChange.getDisplayName() + " " + toChange.getPing());
                                        }
                                        if (Config.doStatCollection) {
                                            for (PlayData playData : bot.uuidsToPlayData.values()) {
                                                int playTimeDifference = (int) (System.currentTimeMillis() - playData.lastPlayed);
                                                playData.playTimeByHour[0] += playTimeDifference;
                                                playData.playTimeByDay[0] += playTimeDifference;
                                                playData.lastPlayed = System.currentTimeMillis();
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                            break;
                        case UPDATE_DISPLAY_NAME:
                            //ignore
                            break;
                        case REMOVE_PLAYER:
                            for (PlayerListEntry entry : pck.getEntries()) {
                                String uuid = entry.getProfile().getId().toString();
                                int removalIndex = -1;
                                for (int i = 0; i < bot.playerListEntries.size(); i++) {
                                    PlayerListEntry player = bot.playerListEntries.get(i);
                                    if (uuid.equals(player.getProfile().getId().toString())) {
                                        removalIndex = i;
                                        if (bot.websocketServer != null) {
                                            bot.websocketServer.sendToAll("tabDel  " + TooBeeTooTeeBot.getName(player));
                                        }
                                        if (Config.doStatCollection) {
                                            bot.uuidsToPlayData.get(uuid).lastPlayed = System.currentTimeMillis();
                                        }
                                        break;
                                    }
                                }
                                if (removalIndex != -1) {
                                    bot.playerListEntries.remove(removalIndex);
                                }
                            }
                            break;
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerListDataPacket) {
                    ServerPlayerListDataPacket pck = packetReceivedEvent.getPacket();
                    bot.tabHeader = pck.getHeader();
                    bot.tabFooter = pck.getFooter();
                    String header = bot.tabHeader.getFullText();
                    String footer = bot.tabFooter.getFullText();
                    if (bot.websocketServer != null) {
                        bot.websocketServer.sendToAll("tabDiff " + header + " " + footer);
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerPositionRotationPacket) {
                    ServerPlayerPositionRotationPacket pck = packetReceivedEvent.getPacket();
                    bot.x = pck.getX();
                    bot.y = pck.getY();
                    bot.z = pck.getZ();
                    bot.yaw = pck.getYaw();
                    bot.pitch = pck.getPitch();
                    bot.client.getSession().send(new ClientTeleportConfirmPacket(pck.getTeleportId()));
                } else if (packetReceivedEvent.getPacket() instanceof ServerChunkDataPacket) {
                    if (Config.doServer) {
                        ServerChunkDataPacket pck = packetReceivedEvent.getPacket();
                        bot.cachedChunks.put(ChunkPos.getChunkHashFromXZ(pck.getColumn().getX(), pck.getColumn().getZ()), pck.getColumn());
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerUnloadChunkPacket) {
                    if (Config.doServer) {
                        ServerUnloadChunkPacket pck = packetReceivedEvent.getPacket();
                        bot.cachedChunks.remove(ChunkPos.getChunkHashFromXZ(pck.getX(), pck.getZ()));
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerUpdateTimePacket) {
                    if (!bot.isLoggedIn) {
                        System.out.println("Logged in!");
                        bot.isLoggedIn = true;
                        if (GuiBot.INSTANCE != null) {
                            GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Logged in!</html>");
                            String[] split = GuiBot.INSTANCE.chatDisplay.getText().split("<br>");
                            if (split.length > 500) {
                                String toSet = "<html>";
                                for (int j = 1; j < split.length; j++) {
                                    toSet += split[j] + "<br>";
                                }
                                toSet = toSet.substring(toSet.length() - 4) + "</html>";
                                GuiBot.INSTANCE.chatDisplay.setText(toSet);
                            }
                        }
                        if (!bot.server.isListening()) {
                            bot.server.bind(true);
                            System.out.println("Started server!");
                            if (GuiBot.INSTANCE != null) {
                                GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Started server!</html>");
                                String[] split = GuiBot.INSTANCE.chatDisplay.getText().split("<br>");
                                if (split.length > 500) {
                                    String toSet = "<html>";
                                    for (int j = 1; j < split.length; j++) {
                                        toSet += split[j] + "<br>";
                                    }
                                    toSet = toSet.substring(toSet.length() - 4) + "</html>";
                                    GuiBot.INSTANCE.chatDisplay.setText(toSet);
                                }
                            }
                        }
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerBlockChangePacket) { //update cached chunks
                    if (Config.doServer) {
                        ServerBlockChangePacket pck = packetReceivedEvent.getPacket();
                        int chunkX = pck.getRecord().getPosition().getX() >> 4;
                        int chunkZ = pck.getRecord().getPosition().getZ() >> 4;
                        int subchunkY = TooBeeTooTeeBot.ensureRange(pck.getRecord().getPosition().getY() >> 4, 0, 15);
                        Column column = bot.cachedChunks.getOrDefault(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), null);
                        if (column == null) {
                            //unloaded or invalid chunk, ignore pls
                            System.out.println("null chunk, this is probably a server bug");
                            break BREAK;
                        }
                        Chunk subChunk = column.getChunks()[subchunkY];
                        int subchunkRelativeY = Math.abs(pck.getRecord().getPosition().getY() - 16 * subchunkY);
                        try {
                            subChunk.getBlocks().set(Math.abs(Math.abs(pck.getRecord().getPosition().getX()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getX() >> 4)) * 16)), TooBeeTooTeeBot.ensureRange(subchunkRelativeY, 0, 15), Math.abs(Math.abs(pck.getRecord().getPosition().getZ()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getZ() >> 4)) * 16)), pck.getRecord().getBlock());
                            column.getChunks()[subchunkY] = subChunk;
                            bot.cachedChunks.put(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), column);
                        } catch (IndexOutOfBoundsException e) {
                            System.out.println((Math.abs(Math.abs(pck.getRecord().getPosition().getX()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getX() >> 4)) * 16))) + " " + subchunkRelativeY + " " + (Math.abs(Math.abs(pck.getRecord().getPosition().getZ()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getZ() >> 4)) * 16))) + " " + (subchunkRelativeY << 8 | chunkZ << 4 | chunkX));
                        }
                        bot.cachedChunks.put(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), column);
                        //System.out.println("chunk " + chunkX + ":" + subchunkY + ":" + chunkZ + " relative pos " + (Math.abs(Math.abs(pck.getRecord().getPosition().getX()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getX() >> 4)) * 16))) + ":" + TooBeeTooTeeBot.ensureRange(subchunkRelativeY, 0, 15) + "(" + subchunkRelativeY + "):" + (Math.abs(pck.getRecord().getPosition().getZ()) - (Math.abs(chunkZ) * 16)) + " original position " + pck.getRecord().getPosition().toString());
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerMultiBlockChangePacket) { //update cached chunks with passion
                    if (Config.doServer) {
                        ServerMultiBlockChangePacket pck = packetReceivedEvent.getPacket();
                        int chunkX = pck.getRecords()[0] //there HAS to be at least one element
                                .getPosition().getX() >> 4; //this cuts away the additional relative chunk coordinates
                        int chunkZ = pck.getRecords()[0] //there HAS to be at least one element
                                .getPosition().getZ() >> 4; //this cuts away the additional relative chunk coordinates
                        Column column = bot.cachedChunks.getOrDefault(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), null);
                        if (column == null) {
                            //unloaded or invalid chunk, ignore pls
                            System.out.println("null chunk multi, this is probably a server bug");
                            break BREAK;
                        }
                        for (BlockChangeRecord record : pck.getRecords()) {
                            int relativeChunkX = Math.abs(Math.abs(record.getPosition().getX()) - (Math.abs(Math.abs(record.getPosition().getX() >> 4)) * 16));
                            int relativeChunkZ = Math.abs(Math.abs(record.getPosition().getZ()) - (Math.abs(Math.abs(record.getPosition().getZ() >> 4)) * 16));
                            int subchunkY = TooBeeTooTeeBot.ensureRange(record.getPosition().getY() >> 4, 0, 15);
                            Chunk subChunk = column.getChunks()[subchunkY];
                            int subchunkRelativeY = Math.abs(record.getPosition().getY() - 16 * subchunkY);
                            try {
                                subChunk.getBlocks().set(relativeChunkX, TooBeeTooTeeBot.ensureRange(subchunkRelativeY, 0, 15), relativeChunkZ, record.getBlock());
                                column.getChunks()[subchunkY] = subChunk;
                            } catch (IndexOutOfBoundsException e) {
                                System.out.println(relativeChunkX + " " + subchunkRelativeY + " " + relativeChunkZ + " " + (subchunkRelativeY << 8 | relativeChunkZ << 4 | relativeChunkX));
                            }
                        }
                        bot.cachedChunks.put(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), column);
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerJoinGamePacket) {
                    ServerJoinGamePacket pck = packetReceivedEvent.getPacket();
                    bot.dimension = pck.getDimension();
                    bot.eid = pck.getEntityId();
                    bot.gameMode = pck.getGameMode();
                } else if (packetReceivedEvent.getPacket() instanceof ServerRespawnPacket) {
                    ServerRespawnPacket pck = packetReceivedEvent.getPacket();
                    bot.dimension = pck.getDimension();
                    bot.cachedChunks.clear();
                } else if (packetReceivedEvent.getPacket() instanceof LoginDisconnectPacket) {
                    LoginDisconnectPacket pck = packetReceivedEvent.getPacket();
                    System.out.println("Kicked during login! Reason: " + pck.getReason().getFullText());
                    bot.client.getSession().disconnect(pck.getReason().getFullText());
                }
            }
            if (Config.doServer) {
                Iterator<PorkClient> iterator = bot.clients.iterator();
                while (iterator.hasNext()) {
                    PorkClient client = iterator.next();
                    if (((MinecraftProtocol) client.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) { //thx 0x kek
                        client.session.send(packetReceivedEvent.getPacket());
                    }
                }
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
    }

    @Override
    public void packetSent(PacketSentEvent packetSentEvent) {
        //System.out.println("Sending " + packetSentEvent.getPacket().getClass().getCanonicalName());
    }

    @Override
    public void connected(ConnectedEvent connectedEvent) {
        System.out.println("Connected to " + Config.ip + ":" + Config.port + "!");
        if (Config.doAntiAFK) {
            bot.timer.schedule(new TimerTask() {
                @Override
                public void run() { //antiafk
                    if (Config.doAntiAFK && bot.clients.size() == 0) {
                        if (bot.r.nextBoolean()) {
                            bot.client.getSession().send(new ClientPlayerSwingArmPacket(Hand.MAIN_HAND));
                        } else {
                            float yaw = -90 + (90 - -90) * bot.r.nextFloat();
                            float pitch = -90 + (90 - -90) * bot.r.nextFloat();
                            bot.client.getSession().send(new ClientPlayerRotationPacket(true, yaw, pitch));
                        }
                    }
                }
            }, 20000, 500);
        }

        if (Config.doSpammer) { //TODO: configurable spam messages
            bot.timer.schedule(new TimerTask() { // i actually want this in a seperate thread, no derp
                @Override
                public void run() { //chat
                    bot.sendChat(Config.spamMesages[bot.r.nextInt(Config.spamMesages.length - 1)]);
                }
            }, 30000, Config.spamDelay);
        }

        bot.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (bot.queuedIngameMessages.size() > 0) {
                    bot.client.getSession().send(new ClientChatPacket(bot.queuedIngameMessages.remove(0)));
                }
            }
        }, 30000, 1000);

        if (Config.doServer && bot.server == null) {
            System.out.println("Starting server...");
            Server server = new Server(Config.serverHost, Config.serverPort, MinecraftProtocol.class, new TcpSessionFactory());
            server.setGlobalFlag(MinecraftConstants.AUTH_PROXY_KEY, Proxy.NO_PROXY);
            server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, Config.doServerAuth);
            server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, new ServerInfoBuilder() {
                @Override
                public ServerStatusInfo buildInfo(Session session) {
                    return new ServerStatusInfo(new VersionInfo(MinecraftConstants.GAME_VERSION, MinecraftConstants.PROTOCOL_VERSION), new PlayerInfo(Integer.MAX_VALUE, bot.clients.size() - 1, new GameProfile[0]), new TextMessage("\u00A7c" + bot.protocol.getProfile().getName()), null);
                }
            });

            server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, new ServerLoginHandler() {
                @Override
                public void loggedIn(Session session) {
                    session.send(new ServerJoinGamePacket(bot.eid, false, bot.gameMode, bot.dimension, Difficulty.NORMAL, 10, WorldType.DEFAULT, false));
                }
            });

            server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 256);
            server.addListener(new PorkServerAdapter(bot));
            bot.server = server;
        }

        if (GuiBot.INSTANCE != null) {
            GuiBot.INSTANCE.connect_disconnectButton.setEnabled(true);
        }

        if (GuiBot.INSTANCE != null) {
            GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Connected to " + Config.ip + ":" + Config.port + "</html>");
            String[] split = GuiBot.INSTANCE.chatDisplay.getText().split("<br>");
            if (split.length > 500) {
                String toSet = "<html>";
                for (int j = 1; j < split.length; j++) {
                    toSet += split[j] + "<br>";
                }
                toSet = toSet.substring(toSet.length() - 4) + "</html>";
                GuiBot.INSTANCE.chatDisplay.setText(toSet);
            }
        }
    }

    @Override
    public void disconnecting(DisconnectingEvent disconnectingEvent) {
        System.out.println("Disconnecting... Reason: " + disconnectingEvent.getReason());
        bot.queuedMessages.add("Disconnecting. Reason: " + disconnectingEvent.getReason());
        if (bot.websocketServer != null) {
            bot.websocketServer.sendToAll("chat    \u00A7cDisconnected from server! Reason: " + disconnectingEvent.getReason());
        }
        if (Config.doWebsocket) {
            TooBeeTooTeeBot.INSTANCE.loginData.setSerializable("registeredPlayers", TooBeeTooTeeBot.INSTANCE.namesToRegisteredPlayers);
            TooBeeTooTeeBot.INSTANCE.loginData.save();
        }
        if (Config.doStatCollection) {
            TooBeeTooTeeBot.INSTANCE.playData.setSerializable("uuidsToPlayData", TooBeeTooTeeBot.INSTANCE.uuidsToPlayData);
            TooBeeTooTeeBot.INSTANCE.playData.save();
        }
        if (Config.doServer) {
            TooBeeTooTeeBot.INSTANCE.server.getSessions().forEach((session) -> {
                session.disconnect("Bot was kicked from server!!!");
            });
        }
        if (GuiBot.INSTANCE != null) {
            GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Disconnectimg from " + Config.ip + ":" + Config.port + "...</html>");
            String[] split = GuiBot.INSTANCE.chatDisplay.getText().split("<br>");
            if (split.length > 500) {
                String toSet = "<html>";
                for (int j = 1; j < split.length; j++) {
                    toSet += split[j] + "<br>";
                }
                toSet = toSet.substring(toSet.length() - 4) + "</html>";
                GuiBot.INSTANCE.chatDisplay.setText(toSet);
            }
        }
    }

    @Override
    public void disconnected(DisconnectedEvent disconnectedEvent) {
        System.out.println("Disconnected.");

        if (GuiBot.INSTANCE != null) {
            if (!GuiBot.INSTANCE.connect_disconnectButton.isEnabled()) {
                GuiBot.INSTANCE.connect_disconnectButton.setEnabled(true);
                return;
            }
        }

        if (GuiBot.INSTANCE != null) {
            GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Disconnected.</html>");
            String[] split = GuiBot.INSTANCE.chatDisplay.getText().split("<br>");
            if (split.length > 500) {
                String toSet = "<html>";
                for (int j = 1; j < split.length; j++) {
                    toSet += split[j] + "<br>";
                }
                toSet = toSet.substring(toSet.length() - 4) + "</html>";
                GuiBot.INSTANCE.chatDisplay.setText(toSet);
            }
        }

        bot.reLaunch();
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
    }
}