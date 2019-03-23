package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Container;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.Protocol.PacketStreamIn;
import net.ME1312.SubData.Client.Protocol.PacketStreamOut;
import net.ME1312.SubData.Client.SubDataClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Packet Forward Packet
 */
public class PacketForwardPacket implements PacketStreamIn, PacketStreamOut {
    private PacketOut packet;
    private UUID id;

    /**
     * New PacketForwardPacket (Out)
     *
     * @param id Client ID
     * @param packet Packet to forward
     */
    public PacketForwardPacket(UUID id, PacketOut packet) {
        this.id = id;
        this.packet = packet;
    }

    @Override
    public void send(SubDataClient client, OutputStream data) throws Throwable {
        data.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(id.getLeastSignificantBits()).array());
        data.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(id.getMostSignificantBits()).array());
        Util.reflect(SubDataClient.class.getDeclaredMethod("write", PacketOut.class, OutputStream.class), client, packet, data);
    }

    @Override
    public void receive(SubDataClient client, InputStream data) throws Throwable {
        Util.reflect(SubDataClient.class.getDeclaredMethod("read", Container.class, InputStream.class), client, new Container<>(false), data);
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
