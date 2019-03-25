package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;

/**
 * Initial Packet for Declaration Class
 */
public final class InitPacketDeclaration implements InitialPacket, PacketIn, PacketOut {
    @Override
    public void receive(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.PRE_INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("state"), client, ConnectionState.INITIALIZATION);
            client.sendPacket(this);
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
