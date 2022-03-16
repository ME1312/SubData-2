package net.ME1312.SubData.Server.Protocol.Internal;

import net.ME1312.SubData.Server.Protocol.PacketStreamIn;
import net.ME1312.SubData.Server.Protocol.PacketStreamOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Packet Forward Packet
 */
public class PacketForwardPacket implements PacketStreamIn, PacketStreamOut {
    private InputStream in;
    private UUID id;

    /**
     * New PacketForwardPacket (In)
     */
    public PacketForwardPacket() {}

    /**
     * New PacketForwardPacket (Out)
     *
     * @param in Packet stream to forward
     */
    public PacketForwardPacket(SubDataClient sender, InputStream in) {
        this.id = sender.getID();
        this.in = in;
    }

    @Override
    public void send(SubDataClient client, OutputStream out) throws Throwable {
        out.write(ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array());
        int i;
        byte[] b = new byte[1024];
        while ((i = in.read(b)) != -1) {
            out.write(b, 0, i);
        }
        out.close();
    }

    @Override
    public void receive(SubDataClient client, InputStream in) throws Throwable {
        ByteBuffer data = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);

        int b, i = 0;
        while ((b = in.read()) != -1) {
            data.put((byte) b);
            if (++i == 16) break;
        }

        data.position(0);
        if (i == 16) {
            UUID id = new UUID(data.getLong(), data.getLong());
            if (client.getServer().getClient(id) != null) {
                client.getServer().getClient(id).sendPacket(new PacketForwardPacket(client, in));
            } else throw new IllegalArgumentException("Cannot forward to invalid Destination ID: [" + id + "]");
        } else {
            throw new IllegalArgumentException("Invalid UUID data for Destination ID: [" + data.getLong() + ", " + data.getLong() + "]");
        }
    }
}
