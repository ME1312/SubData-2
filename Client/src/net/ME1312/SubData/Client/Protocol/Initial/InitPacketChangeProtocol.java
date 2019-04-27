package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Library.DebugUtil;
import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.Protocol.PacketStreamOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataProtocol;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * Initial Packet for Changing Protocols Class
 */
public final class InitPacketChangeProtocol implements InitialPacket, PacketIn, PacketStreamOut {
    @Override
    public void send(SubDataClient client, OutputStream data) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == ConnectionState.INITIALIZATION) {
            Util.reflect(SubDataClient.class.getDeclaredField("state"), client, ConnectionState.READY);

            Util.<Logger>reflect(SubDataClient.class.getDeclaredField("log"), client).info("Logged into " + client.getAddress().toString());

            LinkedList<PacketOut> queue = Util.reflect(SubDataClient.class.getDeclaredField("prequeue"), client);
            if (queue.size() > 0) {
                Util.reflect(SubDataClient.class.getDeclaredField("queue"), client, queue);
            }
            data.close();

            LinkedList<Callback<DataClient>> events = Util.reflect(DataClient.Events.class.getDeclaredField("ready"), client.on);
            Util.reflect(DataClient.Events.class.getDeclaredField("ready"), client.on, new LinkedList<Callback<DataClient>>());
            for (Callback<DataClient> next : events) try {
                if (next != null) next.run(client);
            } catch (Throwable e) {
                DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), Util.reflect(SubDataClient.class.getDeclaredField("log"), client));
            }
        }
    }

    @Override
    public void receive(SubDataClient client) throws Throwable {
        client.sendPacket(this);
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
