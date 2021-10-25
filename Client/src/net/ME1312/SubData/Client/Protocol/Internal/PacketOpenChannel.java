package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.UUID;

/**
 * Open SubChannel Packet
 */
public class PacketOpenChannel implements PacketObjectOut<Integer>, PacketObjectIn<Integer> {
    private final UUID tracker;
    private final boolean response;

    /**
     * New PacketOpenChannel (Out)
     *
     * @param tracker Request Tracker Data
     * @param response Response Status
     */
    public PacketOpenChannel(UUID tracker, boolean response) {
        this.tracker = tracker;
        this.response = response;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender sender) throws Throwable {
        ObjectMap<Integer> response = new ObjectMap<Integer>();
        if (tracker != null) response.set(0x0000, tracker);
        response.set(0x0001, this.response);
        return response;
    }

    @Override
    public void receive(SubDataSender sender, ObjectMap<Integer> data) throws Throwable {
        UUID tracker = (data.contains(0x0000))?data.getUUID(0x0000):null;
        try {
            ((SubDataClient) sender).openChannel().sendPacket(new PacketOpenChannel(tracker, true));
        } catch (Throwable e) {
            sender.sendPacket(new PacketOpenChannel(tracker, false));
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
