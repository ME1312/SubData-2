package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Container.Container;
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
import net.ME1312.SubData.Client.SubDataSender;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Initial Packet for Post Declaration Class
 */
public final class InitPacketPostDeclaration implements InitialProtocol.Packet, PacketObjectIn<Integer>, PacketOut {
    @Override
    public void receive(SubDataSender sender, ObjectMap<Integer> data) throws Throwable {
        UUID clientID =      data.getUUID(0x0000);
        String name =   data.getRawString(0x0001);
        Version version = data.getVersion(0x0002);

        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection()) == ConnectionState.INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("isdcr"), sender.getConnection(), DisconnectReason.PROTOCOL_MISMATCH);
            if (new Container<>(sender.getProtocol().getName()).equals(new Container<>(name))) {
                List<Version> versions = Arrays.asList(sender.getProtocol().getVersion());
                if (versions.contains(version)) {
                    Util.reflect(DataClient.class.getDeclaredField("id"), sender.getConnection(), clientID);
                    sender.sendPacket(this);
                } else {
                    DebugUtil.logException(new IllegalArgumentException("Protocol version mismatch: [" + version + "] is not one of " + versions.toString()), Util.reflect(SubDataClient.class.getDeclaredField("log"), sender.getConnection()));
                    Util.reflect(SubDataClient.class.getDeclaredMethod("close", DisconnectReason.class), sender.getConnection(), DisconnectReason.PROTOCOL_MISMATCH);
                }
            } else {
                DebugUtil.logException(new IllegalArgumentException("Protocol mismatch: [" + name + "] != [" + sender.getProtocol().getName() + "]"), Util.reflect(SubDataClient.class.getDeclaredField("log"), sender.getConnection()));
                Util.reflect(SubDataClient.class.getDeclaredMethod("close", DisconnectReason.class), sender.getConnection(), DisconnectReason.PROTOCOL_MISMATCH);
            }
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
