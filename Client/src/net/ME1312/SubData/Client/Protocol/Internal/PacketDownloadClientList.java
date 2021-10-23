package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Download Client List Packet
 */
public class PacketDownloadClientList implements PacketObjectOut<Integer>, PacketObjectIn<Integer> {
    private static final HashMap<String, Consumer<ObjectMap<String>>[]> callbacks = new HashMap<String, Consumer<ObjectMap<String>>[]>();
    private String tracker;
    private UUID id;

    /**
     * New PacketDownloadClientList (In)
     */
    public PacketDownloadClientList() {}

    /**
     * New PacketDownloadNetworkList (Out)
     *
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadClientList(Consumer<ObjectMap<String>>... callback) {
        Util.nullpo((Object) callback);
        callbacks.put(tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString(), callback);
    }

    /**
     * New PacketDownloadNetworkList (Out)
     *
     * @param id Client ID
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadClientList(UUID id, Consumer<ObjectMap<String>>... callback) {
        Util.nullpo((Object) callback);
        callbacks.put(tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString(), callback);
        this.id = id;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender sender) throws Throwable {
        ObjectMap<Integer> data = new ObjectMap<Integer>();
        data.set(0x0000, tracker);
        if (id != null) data.set(0x0001, id);
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender sender, ObjectMap<Integer> data) throws Throwable {
        for (Consumer<ObjectMap<String>> callback : callbacks.get(data.getRawString(0x0000))) callback.accept(new ObjectMap<String>((Map<String, ?>) data.getObject(0x0001)));
        callbacks.remove(data.getRawString(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
