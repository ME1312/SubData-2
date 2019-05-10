package net.ME1312.SubData.Server.Protocol.Initial;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.ConnectionState;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.Protocol.PacketOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.HashMap;
import java.util.LinkedList;

import static net.ME1312.SubData.Server.Library.ConnectionState.*;

/**
 * Initial Packet for Changing Protocols Class
 */
public final class InitPacketChangeProtocol implements InitialProtocol.Packet, InitialPacket, PacketObjectIn<Integer>, PacketObjectOut<Integer> {

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == INITIALIZATION) {
            data.set(0x0000, Util.<HashMap<ConnectionState, LinkedList<PacketOut>>>reflect(SubDataClient.class.getDeclaredField("statequeue"), client).keySet().contains(POST_INITIALIZATION));
        }
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == INITIALIZATION) {
            HashMap<ConnectionState, LinkedList<PacketOut>> queue = Util.reflect(SubDataClient.class.getDeclaredField("statequeue"), client);

            if (data.getBoolean(0x0000)) {
                Util.reflect(SubDataClient.class.getDeclaredField("state"), client, POST_INITIALIZATION);
                if (queue.size() > 0) {
                    Util.reflect(SubDataClient.class.getDeclaredField("queue"), client, queue.get(POST_INITIALIZATION));
                    Util.reflect(SubDataClient.class.getDeclaredMethod("write"), client);
                }
            } else {
                setReady(client, true);
            }
        } else if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == POST_INITIALIZATION) {
            setReady(client, true);
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
