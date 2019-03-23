package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Protocol.Initial.InitialPacket;
import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;

/**
 * Packet Disconnect Class
 */
public final class PacketDisconnect implements InitialPacket, PacketIn, PacketOut {
    @Override
    public void receive(SubDataClient client) throws Throwable {
        Util.reflect(SubDataClient.class.getDeclaredField("state"), client, ConnectionState.CLOSING);
        client.sendPacket(new PacketDisconnectUnderstood());
        System.out.println("Disconnect");
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
