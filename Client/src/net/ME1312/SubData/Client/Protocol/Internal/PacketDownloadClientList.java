package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Download Client List Packet
 */
public class PacketDownloadClientList implements PacketObjectOut<Integer>, PacketObjectIn<Integer> {
    private static HashMap<String, Callback<ObjectMap<String>>[]> callbacks = new HashMap<String, Callback<ObjectMap<String>>[]>();
    private String tracker;
    private UUID id;

    /**
     * New PacketDownloadNetworkList (Out)
     *
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadClientList(Callback<ObjectMap<String>>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        callbacks.put(tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString(), callback);
    }

    /**
     * New PacketDownloadNetworkList (Out)
     *
     * @param id Client ID
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadClientList(UUID id, Callback<ObjectMap<String>>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        callbacks.put(tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString(), callback);
        this.id = id;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        if (tracker != null) {
            ObjectMap<Integer> data = new ObjectMap<Integer>();
            data.set(0x0000, tracker);
            if (id != null) data.set(0x0001, id);
            return data;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        for (Callback<ObjectMap<String>> callback : callbacks.get(data.getRawString(0x0000))) callback.run(new ObjectMap<String>((Map<String, ?>) data.getObject(0x0001)));
        callbacks.remove(data.getRawString(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
