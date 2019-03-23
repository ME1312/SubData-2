package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.SubDataClient;

import java.util.HashMap;
import java.util.UUID;

/**
 * Download Client List Packet
 */
public class PacketDownloadClientList implements PacketObjectOut, PacketObjectIn {
    private static HashMap<String, Callback<YAMLSection>[]> callbacks = new HashMap<String, Callback<YAMLSection>[]>();
    private String tracker;
    private UUID id;

    /**
     * New PacketDownloadNetworkList (Out)
     *
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketDownloadClientList(Callback<YAMLSection>... callback) {
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
    public PacketDownloadClientList(UUID id, Callback<YAMLSection>... callback) {
        if (Util.isNull((Object) callback)) throw new NullPointerException();
        callbacks.put(tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID).toString(), callback);
        this.id = id;
    }

    @Override
    public YAMLSection send(SubDataClient client) throws Throwable {
        if (tracker != null) {
            YAMLSection data = new YAMLSection();
            data.set("t", tracker);
            if (id != null) data.set("id", id);
            return data;
        } else {
            return null;
        }
    }

    @Override
    public void receive(SubDataClient client, YAMLSection data) throws Throwable {
        for (Callback<YAMLSection> callback : callbacks.get(data.getRawString("t"))) callback.run(data.getSection("r"));
        callbacks.remove(data.getRawString("t"));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
