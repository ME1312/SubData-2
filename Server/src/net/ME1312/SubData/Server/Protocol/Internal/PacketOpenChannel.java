package net.ME1312.SubData.Server.Protocol.Internal;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.HashMap;
import java.util.UUID;

/**
 * Open SubChannel Packet
 */
public class PacketOpenChannel implements PacketObjectOut<Integer>, PacketObjectIn<Integer> {
    private static final HashMap<String, Callback<SubDataClient>[]> callbacks = new HashMap<String, Callback<SubDataClient>[]>();
    private String tracker;

    /**
     * New PacketOpenChannel (In)
     */
    public PacketOpenChannel() {}

    /**
     * New PacketOpenChannel (Out)
     *
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketOpenChannel(Callback<SubDataClient>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        callbacks.put(tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString(), callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        for (Callback<SubDataClient> callback : callbacks.get(data.getRawString(0x0000))) callback.run((data.getBoolean(0x0001))?client:null);
        callbacks.remove(data.getRawString(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
