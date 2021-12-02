package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Library.DebugUtil;
import net.ME1312.SubData.Client.Library.Exception.ProtocolException;
import net.ME1312.SubData.Client.Library.UnsignedData;
import net.ME1312.SubData.Client.Protocol.PacketStreamIn;
import net.ME1312.SubData.Client.Protocol.PacketStreamOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Initial Packet for Declaration Class
 */
public final class InitPacketDeclaration implements InitialProtocol.Packet, PacketStreamIn, PacketStreamOut {
    private static final int version = 0x0005;

    @Override
    public void send(SubDataSender sender, OutputStream data) throws Throwable {
        data.write(UnsignedData.unsign(version, 2), 0 , 2);
        data.close();
    }

    @Override
    public void receive(SubDataSender sender, InputStream data) throws Throwable {
        byte[] pending = new byte[2];
        int version = -1;

        int b, position = 0;
        while (position < 2 && (b = data.read()) != -1) {
            pending[position] = (byte) b;
            if (++position == 2) {
                version = (int) UnsignedData.resign(pending);
            }
        }

        data.close();
        if (InitPacketDeclaration.version != version) {
            if (version < 0) return;
            throw new ProtocolException("SubData protocol version mismatch: [" + DebugUtil.toHex(0xFFFF, version) + "] is not [" + DebugUtil.toHex(0xFFFF, InitPacketDeclaration.version) + "]");
        }

        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection()) == ConnectionState.PRE_INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection(), ConnectionState.INITIALIZATION);
            sender.sendPacket(this);
        }
    }
}
