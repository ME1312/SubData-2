package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.Protocol.Initial.InitialProtocol;
import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;

/**
 * Packet Disconnect Understood Class
 */
public final class PacketDisconnectUnderstood implements InitialProtocol.Packet, PacketIn, PacketOut {
    @Override
    public void receive(SubDataSender sender) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection()) == ConnectionState.CLOSING) {
            Util.reflect(SubDataClient.class.getDeclaredMethod("close", DisconnectReason.class), sender.getConnection(), DisconnectReason.CLOSE_REQUESTED);
        }
    }
}
