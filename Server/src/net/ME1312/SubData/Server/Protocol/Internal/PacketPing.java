package net.ME1312.SubData.Server.Protocol.Internal;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.PingResponse;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

/**
 * Ping Packet
 */
public class PacketPing implements PacketObjectOut<Integer>, PacketObjectIn<Integer> {
    static HashMap<String, Callback<PingResponse>[]> callbacks = new HashMap<String, Callback<PingResponse>[]>();
    private String tracker;
    private long init;

    /**
     * New PacketPing (In)
     */
    public PacketPing() {}

    /**
     * New PacketPing (Out)
     *
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketPing(Callback<PingResponse>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        init = Calendar.getInstance().getTime().getTime();
        callbacks.put(tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString(), callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, init); // Object Initialization (Local)
        data.set(0x0002, Calendar.getInstance().getTime().getTime()); // On Send (Local)
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        client.sendPacket(new PacketPingResponse(data));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
