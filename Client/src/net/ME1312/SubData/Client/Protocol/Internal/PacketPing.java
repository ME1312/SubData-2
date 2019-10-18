package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.PingResponse;
import net.ME1312.SubData.Client.Protocol.Forwardable;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

/**
 * Ping Packet
 */
public class PacketPing implements Forwardable, PacketObjectOut<Integer>, PacketObjectIn<Integer> {
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
    public ObjectMap<Integer> send(SubDataSender sender) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        data.set(0x0001, init); // Object Initialization (Local)
        data.set(0x0002, Calendar.getInstance().getTime().getTime()); // On Send (Local)
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender sender, ObjectMap<Integer> data) throws Throwable {
        sender.sendPacket(new PacketPingResponse(data));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
