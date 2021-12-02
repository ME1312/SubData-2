package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Protocol.Initial.InitialProtocol;
import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;

/**
 * Packet Disconnect Class
 */
public final class PacketDisconnect implements InitialProtocol.Packet, PacketIn, PacketOut {
    @Override
    public void receive(SubDataSender sender) throws Throwable {
        Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection(), ConnectionState.CLOSING);
        sender.getConnection().sendPacket(new PacketDisconnectUnderstood());
    }
}
