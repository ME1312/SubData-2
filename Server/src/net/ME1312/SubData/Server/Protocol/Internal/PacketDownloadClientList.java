package net.ME1312.SubData.Server.Protocol.Internal;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.PacketObjectIn;
import net.ME1312.SubData.Server.Protocol.PacketObjectOut;
import net.ME1312.SubData.Server.SerializableClientHandler;
import net.ME1312.SubData.Server.SubDataClient;

import java.util.Map;
import java.util.UUID;

/**
 * Download Client List Packet
 */
public class PacketDownloadClientList implements PacketObjectOut, PacketObjectIn {
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
    public YAMLSection send(SubDataClient client) throws Throwable {
        Map<UUID, ? extends SubDataClient> clients = client.getServer().getClients();
        YAMLSection response = new YAMLSection();
        YAMLSection data = new YAMLSection();

        for (UUID id : clients.keySet()) if (request == null || request == id) {
            data.set(id.toString(), (clients.get(id).getHandler() instanceof SerializableClientHandler)? Util.getDespiteException(((SerializableClientHandler) clients.get(id).getHandler())::forSubData, null):null);
        }

        if (tracker != null) response.set("t", tracker);
        response.set("r", data);
        return response;
    }

    @Override
    public void receive(SubDataClient client, YAMLSection data) throws Throwable {
        client.sendPacket(new PacketDownloadClientList((data.contains("t"))?data.getRawString("t"):null, (data.contains("id"))?data.getUUID("id"):null));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
