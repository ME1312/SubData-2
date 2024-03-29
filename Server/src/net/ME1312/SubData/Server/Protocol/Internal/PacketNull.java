package net.ME1312.SubData.Server.Protocol.Internal;

import net.ME1312.SubData.Server.Protocol.Initial.InitialProtocol;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketOut;
import net.ME1312.SubData.Server.SubDataClient;

/**
 * Packet for Doing Literally Nothing
 */
public final class PacketNull implements InitialProtocol.Packet, PacketIn, PacketOut {
    public boolean sent = false;

    @Override
    public void sending(SubDataClient client) {
        sent = true;
    }

    @Override
    public void receive(SubDataClient sender) throws Throwable {
        // do nothing
    }
}
