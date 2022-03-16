package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Library.DebugUtil;
import net.ME1312.SubData.Server.Library.Exception.ProtocolException;
import net.ME1312.SubData.Server.Library.UnsignedData;
import net.ME1312.SubData.Server.Protocol.PacketStreamIn;
import net.ME1312.SubData.Server.Protocol.PacketStreamOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Initial Packet for Declaration Class
 */
public final class InitPacketDeclaration implements InitialProtocol.Packet, PacketStreamIn, PacketStreamOut {
    private static final int version = 0x0005;

    @Override
    public void send(SubDataClient client, OutputStream data) throws Throwable {
        data.write(UnsignedData.unsign(version, 2), 0, 2);
        data.close();
    }

    @Override
    public void receive(SubDataClient client, InputStream in) throws Throwable {
        byte[] data = new byte[2];

        int b, i = 0;
        while ((b = in.read()) != -1) {
            data[i] = (byte) b;
            if (++i == 2) break;
        }
        in.close();

        int version = (int) UnsignedData.resign(data);
        if (InitPacketDeclaration.version != version) {
            if (version < 0) return;
            throw new ProtocolException("SubData protocol version mismatch: [" + DebugUtil.toHex(0xFFFF, version) + "] is not [" + DebugUtil.toHex(0xFFFF, InitPacketDeclaration.version) + "]");
        }

        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.PRE_INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("state"), client, ConnectionState.INITIALIZATION);
            client.sendPacket(new InitPacketChangeEncryption());
        }
    }
}
