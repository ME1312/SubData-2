package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;

import java.util.HashMap;
import java.util.LinkedList;

import static net.ME1312.SubData.Client.Library.ConnectionState.POST_INITIALIZATION;

/**
 * Initial Packet for Verifying State Class
 */
public final class InitPacketVerifyState implements InitialProtocol.Packet, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    boolean postinit;

    public InitPacketVerifyState() {}
    private InitPacketVerifyState(boolean postinit) {
        this.postinit = postinit;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), client) == POST_INITIALIZATION) {
            HashMap<ConnectionState, LinkedList<PacketOut>> queue = Util.reflect(SubDataClient.class.getDeclaredField("statequeue"), client);

            data.set(0x0000, true);
            data.set(0x0001, postinit || (queue.keySet().contains(POST_INITIALIZATION) && queue.get(POST_INITIALIZATION).size() > 0));
            if (data.getBoolean(0x0001)) {
                if (queue.get(POST_INITIALIZATION).size() > 0) {
                    Util.reflect(SubDataClient.class.getDeclaredField("queue"), client, queue.get(POST_INITIALIZATION));
                }
            } else {
                setReady(client, false);
            }
        } else {
            data.set(0x0000, Util.<ConnectionState>reflect(SubDataClient.class.getDeclaredField("state"), client).asInt() > POST_INITIALIZATION.asInt());
            data.set(0x0001, false);
        }
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        client.sendPacket(new InitPacketVerifyState(data.getBoolean(0x0000, false)));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
