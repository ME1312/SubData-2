package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Container;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
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
import java.util.UUID;

/**
 * Initial Packet for Post Declaration Class
 */
public final class InitPacketPostDeclaration implements InitialPacket, PacketObjectIn<Integer>, PacketOut {
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        UUID clientID =      data.getUUID(0x0000);
        String name =   data.getRawString(0x0001);
        Version version = data.getVersion(0x0002);

        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.INITIALIZATION) {
            if (new Container<>(client.getProtocol().getName()).equals(new Container<>(name))) {
                List<Version> versions = Arrays.asList(client.getProtocol().getVersion());
                if (versions.contains(version)) {
                    Util.reflect(DataClient.class.getDeclaredField("id"), client, clientID);
                    client.sendPacket(this);
                } else {
                    DebugUtil.logException(new IllegalArgumentException("Protocol version mismatch: [" + version + "] is not one of " + versions.toString()), Util.reflect(SubDataClient.class.getDeclaredField("log"), client));
                    Util.reflect(SubDataClient.class.getDeclaredMethod("close", DisconnectReason.class), client, DisconnectReason.PROTOCOL_MISMATCH);
                }
            } else {
                DebugUtil.logException(new IllegalArgumentException("Protocol mismatch: [" + name + "] != [" + client.getProtocol().getName() + "]"), Util.reflect(SubDataClient.class.getDeclaredField("log"), client));
                Util.reflect(SubDataClient.class.getDeclaredMethod("close", DisconnectReason.class), client, DisconnectReason.PROTOCOL_MISMATCH);
            }
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
