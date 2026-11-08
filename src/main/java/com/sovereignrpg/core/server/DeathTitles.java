package com.sovereignrpg.core.server;

import com.sovereignrpg.core.CoreConfig;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Souls Like Bosses sends a big title when a boss dies ("NIGHTLORD SLAIN", "LORD OF CINDER FALLEN", ...). Those are plain
 * title packets with no setting to turn them off, so this drops exactly those packets on the way out to each player.
 */
@Mod.EventBusSubscriber(modid = "sovereignrpgcore")
public final class DeathTitles {
    private DeathTitles() {}

    private static final String NAME = "sovereignrpgcore_death_titles";

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.connection == null) return;
        try {
            ChannelPipeline pipe = sp.connection.connection.channel().pipeline();
            if (pipe.get(NAME) != null) return;
            pipe.addBefore("packet_handler", NAME, new ChannelDuplexHandler() {
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    if (msg instanceof ClientboundSetTitleTextPacket p && CoreConfig.HIDE_DEATH_TITLES.get()) {
                        String text = p.getText().getString();
                        for (String t : CoreConfig.DEATH_TITLES.get()) {
                            if (t.equalsIgnoreCase(text)) {
                                promise.setSuccess();
                                return;
                            }
                        }
                    }
                    super.write(ctx, msg, promise);
                }
            });
        } catch (Throwable t) {
            // the titles just stay visible if the pipeline layout is different
        }
    }
}
