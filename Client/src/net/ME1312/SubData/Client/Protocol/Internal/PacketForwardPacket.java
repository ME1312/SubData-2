package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.ForwardedDataSender;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.Protocol.PacketStreamIn;
import net.ME1312.SubData.Client.Protocol.PacketStreamOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Packet Forward Packet
 */
public class PacketForwardPacket implements PacketStreamIn, PacketStreamOut {
    private final PacketOut packet;
    private final UUID id;

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
    public void send(SubDataSender sender, OutputStream data) throws Throwable {
        data.write(ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array());
        Util.reflect(SubDataClient.class.getDeclaredMethod("write", SubDataSender.class, PacketOut.class, OutputStream.class), sender.getConnection(), new ForwardedDataSender(sender.getConnection(), id), packet, data);
    }

    @Override
    public void receive(SubDataSender sender, InputStream in) throws Throwable {
        ByteBuffer data = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);

        int b, i = 0;
        while ((b = in.read()) != -1) {
            data.put((byte) b);
            if (++i == 16) break;
        }

        data.position(0);
        if (i == 16) {
            Util.reflect(SubDataClient.class.getDeclaredMethod("read", SubDataSender.class, Container.class, InputStream.class), sender.getConnection(), new ForwardedDataSender(sender.getConnection(), new UUID(data.getLong(), data.getLong())), new Container<>(false), in);
        } else {
            throw new IllegalArgumentException("Invalid UUID data for Sender ID: [" + data.getLong() + ", " + data.getLong() + "]");
        }
    }
}
