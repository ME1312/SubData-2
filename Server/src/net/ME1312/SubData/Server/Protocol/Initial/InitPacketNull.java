package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketOut;
import net.ME1312.SubData.Server.SubDataClient;

/**
 * Initial Packet for Doing Literally Nothing
 */
public final class InitPacketNull implements InitialProtocol.Packet, PacketIn, PacketOut {

    @Override
    public void receive(SubDataClient sender) throws Throwable {
        // do nothing
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
