package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Library.DebugUtil;
import net.ME1312.SubData.Server.Protocol.PacketOut;
import net.ME1312.SubData.Server.SubDataClient;
import net.ME1312.SubData.Server.SubDataServer;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static net.ME1312.SubData.Server.Library.ConnectionState.READY;

/**
 * InitialPacket Tag Class<br>
 * Classes that implement this may be sent during the POST_INITIALIZATION state
 */
public interface InitialPacket {

    /**
     * Change the state of a Client to READY
     *
     * @param client Client
     * @throws Throwable
     */
    default void setReady(SubDataClient client) throws Throwable {
        if (Util.<ConnectionState>reflect(SubDataClient.class.getDeclaredField("state"), client).asInt() < READY.asInt()) {
            Util.reflect(SubDataClient.class.getDeclaredField("state"), client, ConnectionState.READY);

            Util.<Logger>reflect(SubDataServer.class.getDeclaredField("log"), client.getServer()).info(client.getAddress().toString() + " has logged in");

            HashMap<ConnectionState, LinkedList<PacketOut>> queue = Util.reflect(SubDataClient.class.getDeclaredField("statequeue"), client);
            if (queue.containsKey(READY)) {
                if (queue.get(READY).size() > 0) {
                    client.sendPacket(queue.get(READY).toArray(new PacketOut[0]));
                }
                queue.remove(READY);
            }

            LinkedList<Consumer<DataClient>> events = new LinkedList<>(Util.reflect(DataClient.Events.class.getDeclaredField("ready"), client.on));
            for (Consumer<DataClient> next : events) try {
                if (next != null) next.accept(client);
            } catch (Throwable e) {
                DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), Util.reflect(SubDataServer.class.getDeclaredField("log"), client.getServer()));
            }
        }
    }
}
