package net.ME1312.SubData.Server.Protocol.Internal;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.Map;
import java.util.UUID;

/**
 * Download Client List Packet
 */
public class PacketDownloadClientList implements PacketObjectOut<Integer>, PacketObjectIn<Integer> {
    private String tracker;
    private UUID request;

    /**
     * New PacketDownloadClientList (Out)
     *
     * @param tracker Request Tracker Data
     * @param request Client ID
     */
    public PacketDownloadClientList(String tracker, UUID request) {
        this.tracker = tracker;
        this.request = request;
    }

    @Override
    public ObjectMap<Integer> send(SubDataClient client) throws Throwable {
        Map<UUID, ? extends SubDataClient> clients = client.getServer().getClients();
        ObjectMap<Integer> response = new ObjectMap<Integer>();
        ObjectMap<String> data = new ObjectMap<String>();

        for (UUID id : clients.keySet()) if (request == null || request == id) {
            data.set(id.toString(), Util.getDespiteException(() -> clients.get(id).getHandler().forSubData(), null));
        }

        if (tracker != null) response.set(0x0000, tracker);
        response.set(0x0001, data);
        return response;
    }

    @Override
    public void receive(SubDataClient client, ObjectMap<Integer> data) throws Throwable {
        client.sendPacket(new PacketDownloadClientList((data.contains(0x0000))?data.getRawString(0x0000):null, (data.contains(0x0001))?data.getUUID(0x0001):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
