package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.SubData.Client.Protocol.Initial.InitialProtocol;
import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataSender;

/**
 * Packet for Doing Literally Nothing
 */
public final class PacketNull implements InitialProtocol.Packet, PacketIn, PacketOut {
    public boolean sent = false;

    @Override
    public void sending(SubDataSender client) throws Throwable {
        sent = true;
    }

    @Override
    public void receive(SubDataSender sender) throws Throwable {
        // do nothing
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
