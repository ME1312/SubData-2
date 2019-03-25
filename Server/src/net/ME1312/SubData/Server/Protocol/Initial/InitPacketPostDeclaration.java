package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

/**
 * Initial Packet for Post Declaration Class
 */
public final class InitPacketPostDeclaration implements InitialPacket, PacketIn, PacketObjectOut<Integer> {
    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, client.getID());
        data.set(0x0001, client.getServer().getProtocol().getName());
        data.set(0x0002, client.getServer().getProtocol().getVersion());
        return data;
    }

    @Override
    public void receive(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.INITIALIZATION) {
            client.sendPacket(new InitPacketChangeProtocol());
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}