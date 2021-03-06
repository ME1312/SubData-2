package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Library.DebugUtil;
import net.ME1312.SubData.Client.Library.Exception.ProtocolException;
import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;

/**
 * Initial Packet for Declaration Class
 */
public final class InitPacketDeclaration implements InitialProtocol.Packet, PacketIn, PacketOut {
    @Override
    public void receive(SubDataSender sender) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection()) == ConnectionState.PRE_INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection(), ConnectionState.INITIALIZATION);
            sender.sendPacket(this);
        }
    }

    @Override
    public int version() {
        return 0x0004;
    }

    @Override
    public boolean isCompatible(int version) {
        if (version() != version) throw new ProtocolException("SubData protocol version mismatch: [" + DebugUtil.toHex(0xFFFF, version) + "] is not [" + DebugUtil.toHex(0xFFFF, version()) + "]");
        return true;
    }
}
