/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import lombok.NonNull;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.toobeetooteebot.Bot;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

import static net.daporkchop.toobeetooteebot.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class PlayerHealthHandler implements HandlerRegistry.IncomingHandler<ServerPlayerHealthPacket, PorkClientSession> {
    @Override
    public boolean apply(@NonNull ServerPlayerHealthPacket packet, @NonNull PorkClientSession session) {
        CACHE.getPlayerCache().getThePlayer()
                .setFood(packet.getFood())
                .setSaturation(packet.getSaturation())
                .setHealth(packet.getHealth());
        CACHE_LOG.debug("Player food: %d", packet.getFood())
                .debug("Player saturation: %f", packet.getSaturation())
                .debug("Player health: %f", packet.getHealth());

        if (CONFIG.client.extra.autoDisconnect.enabled && CONFIG.client.extra.autoDisconnect.health > packet.getHealth()) {
            Bot.getInstance().getClient().getSession().disconnect("Low health");
        }


        if (packet.getHealth() <= 0) {

            if (CONFIG.client.extra.autoRespawn.enabled) {
                Bot.getInstance().getDiscordBot().onDeath("[Death] I just died");
                new Thread(() -> {
                    PorkUtil.sleep(CONFIG.client.extra.autoRespawn.delayMillis);
                    if (Bot.getInstance().isConnected() && CACHE.getPlayerCache().getThePlayer().getHealth() <= 0) {
                        Bot.getInstance().getClient().getSession().send(new ClientRequestPacket(ClientRequest.RESPAWN));
                    }
                }).start();
            } else {
                Bot.getInstance().getDiscordBot().onDeath("[Death] I just died");
                Bot.getInstance().getClient().getSession().disconnect("death");
            }
        }
        return true;
    }

    @Override
    public Class<ServerPlayerHealthPacket> getPacketClass() {
        return ServerPlayerHealthPacket.class;
    }
}
