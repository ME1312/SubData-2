package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Protocol.*;
import net.ME1312.SubData.Client.SubDataClient;

import java.util.HashMap;
import java.util.LinkedList;

import static net.ME1312.SubData.Client.Library.ConnectionState.*;

/**
 * Initial Packet for Changing Protocols Class
 */
public final class InitPacketChangeProtocol implements InitialProtocol.Packet, InitialPacket, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    boolean postinit;

    public InitPacketChangeProtocol() {}
    private InitPacketChangeProtocol(boolean postinit) {
        this.postinit = postinit;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == INITIALIZATION) {
            HashMap<ConnectionState, LinkedList<PacketOut>> queue = Util.reflect(SubDataClient.class.getDeclaredField("statequeue"), client);

            data.set(0x0000, postinit || queue.keySet().contains(POST_INITIALIZATION));
            if (data.getBoolean(0x0000)) {
                Util.reflect(SubDataClient.class.getDeclaredField("state"), client, POST_INITIALIZATION);
                if (queue.size() > 0) {
                    Util.reflect(SubDataClient.class.getDeclaredField("queue"), client, queue.get(POST_INITIALIZATION));
                }
            } else {
                setReady(client, false);
            }
        } else if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == POST_INITIALIZATION) {
            setReady(client, false);
        }
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        client.sendPacket(new InitPacketChangeProtocol(data.getBoolean(0x0000, false)));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
