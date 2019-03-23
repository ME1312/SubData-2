package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.SubDataProtocol;

import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * Initial Packet for Changing Protocols Class
 */
public final class InitPacketChangeProtocol implements InitialPacket, PacketIn, PacketOut {
    @Override
    public void receive(SubDataClient client) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("state"), client, ConnectionState.READY);

            Util.<Logger>reflect(SubDataProtocol.class.getDeclaredField("log"), client.getServer().getProtocol()).info(client.getAddress().toString() + " has logged in");

            LinkedList<PacketOut> queue = Util.reflect(SubDataClient.class.getDeclaredField("prequeue"), client);
            if (queue.size() > 0) {
                Util.reflect(SubDataClient.class.getDeclaredField("queue"), client, queue);
                Util.reflect(SubDataClient.class.getDeclaredMethod("write"), client);
            }
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
