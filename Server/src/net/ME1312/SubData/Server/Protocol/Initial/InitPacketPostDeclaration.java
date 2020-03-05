package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Library.DisconnectReason;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

/**
 * Initial Packet for Post Declaration Class
 */
public final class InitPacketPostDeclaration implements InitialProtocol.Packet, PacketIn, PacketObjectOut<Integer> {
    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("isdcr"), client, DisconnectReason.PROTOCOL_MISMATCH);
            ObjectMap<Integer> data = new ObjectMap<Integer>();
            data.set(0x0000, client.getID());
            data.set(0x0001, client.getServer().getProtocol().getName());
            data.set(0x0002, client.getServer().getProtocol().getVersion());
            return data;
        } else return null;
    }

    @Override
    public void receive(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.INITIALIZATION) {
            client.sendPacket(new InitPacketLogin());
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
