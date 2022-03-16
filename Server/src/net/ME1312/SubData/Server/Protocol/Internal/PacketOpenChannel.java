package net.ME1312.SubData.Server.Protocol.Internal;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Open SubChannel Packet
 */
public class PacketOpenChannel implements PacketObjectOut<Integer>, PacketObjectIn<Integer> {
    private static final HashMap<UUID, Consumer<SubDataClient>[]> callbacks = new HashMap<UUID, Consumer<SubDataClient>[]>();
    private UUID tracker;

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
    public PacketOpenChannel(Consumer<SubDataClient>... callback) {
        Util.nullpo((Object) callback);
        callbacks.put(tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID), callback);
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        return data;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        for (Consumer<SubDataClient> callback : callbacks.remove(data.getUUID(0x0000))) callback.accept((data.getBoolean(0x0001))?client:null);
    }
}
