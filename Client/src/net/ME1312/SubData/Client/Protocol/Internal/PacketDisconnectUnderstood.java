package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.Protocol.Initial.InitialPacket;
import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;

/**
 * Packet Disconnect Understood Class
 */
public final class PacketDisconnectUnderstood implements InitialPacket, PacketIn, PacketOut {
    @Override
    public void receive(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.CLOSING) {
            Util.reflect(SubDataClient.class.getDeclaredMethod("close", DisconnectReason.class), client, DisconnectReason.CLOSE_REQUESTED);
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
