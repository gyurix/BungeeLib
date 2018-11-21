package gyurix.bungeelib.protocol;

import gyurix.bungeelib.utils.BU;
import gyurix.bungeelib.utils.Reflection;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.PacketWrapper;

import java.lang.reflect.Field;

import static gyurix.bungeelib.utils.BU.bl;

public class ProtocolAPI implements Listener {
    private static final Field initialHandlerChannelF = Reflection.getField(InitialHandler.class, "ch");
    private static final Field userConnectionHandlerChannelF = Reflection.getField(UserConnection.class, "ch");

    public ProtocolAPI() {
        BU.pm.registerListener(bl, this);
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent e) throws Throwable {
        ChannelWrapper chw = (ChannelWrapper) initialHandlerChannelF.get(e.getConnection());
        chw.addBefore("inbound-boss", "bungeelib", new BungeeLibHandler());
    }

    public static class BungeeLibHandler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof PacketWrapper) {
                PacketWrapper pw = (PacketWrapper) msg;
                if (pw.packet == null) {
                    ctx.fireChannelRead(msg);
                    return;
                }
                //BU.log(bl, "Read", pw.packet.toString());
                ctx.fireChannelRead(msg);
                return;
            }
            //BU.log(bl, "PacketAPI - Read - " + msg);
            ctx.fireChannelRead(msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if (msg instanceof ByteBuf) {
                ctx.write(msg, promise);
                return;
            }
            //BU.log(bl, "PacketAPI - Write - " + msg);
            ctx.write(msg, promise);
        }
    }
}
