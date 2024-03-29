package net.ME1312.SubData.Client.Protocol.Initial;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.ConnectionState;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.HashMap;
import java.util.LinkedList;

import static net.ME1312.SubData.Client.Library.ConnectionState.POST_INITIALIZATION;

/**
 * Initial Packet for Verifying State Class
 */
public final class InitPacketVerifyState implements InitialProtocol.Packet, PacketObjectIn<Integer>, PacketObjectOut<Integer> {
    private boolean postinit;

    public InitPacketVerifyState() {}
    private InitPacketVerifyState(boolean postinit) {
        this.postinit = postinit;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender sender) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        if (Util.reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection()) == POST_INITIALIZATION) {
            HashMap<ConnectionState, LinkedList<PacketOut>> queue = Util.reflect(SubDataClient.class.getDeclaredField("statequeue"), sender.getConnection());

            data.set(0x0000, true);
            data.set(0x0001, postinit || (queue.containsKey(POST_INITIALIZATION) && queue.get(POST_INITIALIZATION).size() > 0));
            if (data.getBoolean(0x0001)) {
                if (queue.containsKey(POST_INITIALIZATION)) {
                    if (queue.get(POST_INITIALIZATION).size() > 0) {
                        sender.getConnection().sendPacket(queue.get(POST_INITIALIZATION).toArray(new PacketOut[0]));
                    }
                    queue.remove(POST_INITIALIZATION);
                }
            } else {
                setReady(sender.getConnection());
            }
        } else {
            data.set(0x0000, Util.<ConnectionState>reflect(SubDataClient.class.getDeclaredField("state"), sender.getConnection()).asInt() > POST_INITIALIZATION.asInt());
            data.set(0x0001, false);
        }
        return data;
    }

    @Override
    public void receive(SubDataSender sender, ObjectMap<Integer> data) throws Throwable {
        sender.sendPacket(new InitPacketVerifyState(data.getBoolean(0x0000, false)));
    }
}
