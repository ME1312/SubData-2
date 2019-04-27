package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Library.DebugUtil;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.SubDataProtocol;
import net.ME1312.SubData.Server.SubDataServer;

import java.lang.reflect.InvocationTargetException;
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

            Util.<Logger>reflect(SubDataServer.class.getDeclaredField("log"), client.getServer()).info(client.getAddress().toString() + " has logged in");

            LinkedList<PacketOut> queue = Util.reflect(SubDataClient.class.getDeclaredField("prequeue"), client);
            if (queue.size() > 0) {
                Util.reflect(SubDataClient.class.getDeclaredField("queue"), client, queue);
                Util.reflect(SubDataClient.class.getDeclaredMethod("write"), client);
            }

            LinkedList<Callback<DataClient>> events = Util.reflect(DataClient.Events.class.getDeclaredField("ready"), client.on);
            Util.reflect(DataClient.Events.class.getDeclaredField("ready"), client.on, new LinkedList<Callback<DataClient>>());
            for (Callback<DataClient> next : events) try {
                if (next != null) next.run(client);
            } catch (Throwable e) {
                DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), Util.reflect(SubDataServer.class.getDeclaredField("log"), client.getServer()));
            }
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
