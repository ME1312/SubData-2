package net.ME1312.SubData.Server.Protocol.Internal;

import net.ME1312.SubData.Server.Protocol.PacketStreamIn;
import net.ME1312.SubData.Server.Protocol.PacketStreamOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.io.ByteArrayOutputStream;
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
        out.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(id.getMostSignificantBits()).array());
        out.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(id.getLeastSignificantBits()).array());
        int b;
        while ((b = in.read()) != -1) out.write(b);
        out.close();
    }

    @Override
    public void receive(SubDataClient client, InputStream in) throws Throwable {
        ByteArrayOutputStream pending = new ByteArrayOutputStream();
        long id_p1 = -1, id_p2 = -1;

        int b, position = 0;
        while (position < 16 && (b = in.read()) != -1) {
            position++;
            pending.write(b);
            switch (position) {
                case 8:
                    id_p1 = ByteBuffer.wrap(pending.toByteArray()).order(ByteOrder.BIG_ENDIAN).getLong();
                    pending.reset();
                    break;
                case 16:
                    id_p2 = ByteBuffer.wrap(pending.toByteArray()).order(ByteOrder.BIG_ENDIAN).getLong();
                    pending.reset();
                    break;
            }
        }

        if (position >= 16) {
            UUID id = new UUID(id_p1, id_p2);
            if (client.getServer().getClient(id) != null) {
                client.getServer().getClient(id).sendPacket(new PacketForwardPacket(client, in));
            } else throw new IllegalArgumentException("Cannot forward to invalid Destination ID: [" + id.toString() + "]");
        } else {
            throw new IllegalArgumentException("Invalid UUID data for Destination ID: [" + id_p1 + ", " + id_p2 + "]");
        }
    }

    @Override
    public int version() {
        return 0x0002;
    }
}
