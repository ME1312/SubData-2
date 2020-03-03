package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataSender;

/**
 * Initial Packet for Doing Literally Nothing
 */
public final class InitPacketNull implements InitialProtocol.Packet, PacketIn, PacketOut {

    @Override
    public void receive(SubDataSender sender) throws Throwable {
        // do nothing
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
