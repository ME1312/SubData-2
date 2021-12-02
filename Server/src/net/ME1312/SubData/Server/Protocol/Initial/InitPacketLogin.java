package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.AuthService;
import net.ME1312.SubData.Server.ClientHandler;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Library.DebugUtil;
import net.ME1312.SubData.Server.Library.DisconnectReason;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.SubDataServer;

import java.lang.reflect.InvocationTargetException;

/**
 * Initial Packet for Login Class
 */
public final class InitPacketLogin implements InitialProtocol.Packet, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("isdcr"), client, DisconnectReason.INVALID_LOGIN);
            ObjectMap<Integer> data = new ObjectMap<Integer>();
            data.set(0x0000, client.getServer().getProtocol().getAuthService() != null);
            return data;
        } else return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.INITIALIZATION) {
            AuthService<?> service = client.getServer().getProtocol().getAuthService();
            if (service != null) {
                Object asr = null;
                ObjectMap login = (data != null && data.contains(0x0000))?data.getMap(0x0000):null;
                try {
                    asr = service.authenticate(client, login);
                } catch (Throwable e) {
                    DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData AuthService"), Util.reflect(SubDataServer.class.getDeclaredField("log"), client.getServer()));
                }
                if (asr != null) {
                    Util.reflect(SubDataClient.class.getDeclaredField("asr"), client, asr);
                    if (asr instanceof ClientHandler) client.setHandler((ClientHandler) asr);
                    client.sendPacket(new InitPacketChangeState());
                } else {
                    Util.reflect(SubDataClient.class.getDeclaredMethod("close", DisconnectReason.class), client, DisconnectReason.INVALID_LOGIN);
                }
            } else {
                client.sendPacket(new InitPacketChangeState());
            }
        }
    }
}
