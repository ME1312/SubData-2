package net.ME1312.SubData.Server.Protocol.Internal;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Protocol.Initial.InitialProtocol;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketOut;
import net.ME1312.SubData.Server.SubDataClient;

/**
 * Packet Disconnect Class
 */
public final class PacketDisconnect implements InitialProtocol.Packet, PacketIn, PacketOut {
    @Override
    public void receive(SubDataClient client) throws Throwable {
        Util.reflect(SubDataClient.class.getDeclaredField("state"), client, ConnectionState.CLOSING);
        client.sendPacket(new PacketDisconnectUnderstood());
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
