package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Library.DebugUtil;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataProtocol;

import java.util.Arrays;
import java.util.List;

/**
 * Initial Packet for Declaration Class
 */
public final class InitPacketDeclaration implements InitialPacket, PacketObjectIn, PacketOut {
    @Override
    public void receive(SubDataClient client, YAMLSection data) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.PRE_INITIALIZATION) {
            List<Version> versions = Arrays.asList(client.getProtocol().getVersion());
            if (versions.contains(data.getVersion("v"))) {
                Util.reflect(DataClient.class.getDeclaredField("id"), client, data.getUUID("id"));
                Util.reflect(SubDataClient.class.getDeclaredField("state"), client, ConnectionState.INITIALIZATION);
                client.sendPacket(this);
            } else {
                DebugUtil.logException(new IllegalArgumentException("Protocol version mismatch: [" + data.getVersion("v") + "] is not one of " + versions.toString()), Util.reflect(SubDataProtocol.class.getDeclaredField("log"), client.getProtocol()));
                Util.reflect(SubDataClient.class.getDeclaredMethod("close", DisconnectReason.class), client, DisconnectReason.PROTOCOL_MISMATCH);
            }
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
