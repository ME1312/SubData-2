package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.PingResponse;
import net.ME1312.SubData.Client.Protocol.Forwardable;
import net.ME1312.SubData.Client.Protocol.PacketStreamIn;
import net.ME1312.SubData.Client.Protocol.PacketStreamOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Ping Packet
 */
public class PacketPing implements Forwardable, PacketStreamOut, PacketStreamIn {
    static HashMap<UUID, Consumer<PingResponse>[]> callbacks = new HashMap<UUID, Consumer<PingResponse>[]>();
    static HashMap<UUID, PacketPing> data = new HashMap<UUID, PacketPing>();
    private UUID tracker;
    long init, queue;

    /**
     * New PacketPing (In)
     */
    public PacketPing() {}

    /**
     * New PacketPing (Out)
     *
     * @param callback Callbacks
     */
    @SafeVarargs
    public PacketPing(Consumer<PingResponse>... callback) {
        Util.nullpo((Object) callback);
        init = Calendar.getInstance().getTime().getTime();
        callbacks.put(tracker = Util.getNew(callbacks.keySet(), UUID::randomUUID), callback);
    }

    @Override
    public void send(SubDataSender sender, OutputStream out) throws Throwable {
        data.put(tracker, this);
        queue = Calendar.getInstance().getTime().getTime();

        out.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(tracker.getMostSignificantBits()).array());
        out.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(tracker.getLeastSignificantBits()).array());
        out.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(init).array());  // Object Initialization (Local) [0]
        out.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(queue).array()); // On Send (Local) [1]
        out.close();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender sender, InputStream in) throws Throwable {
        ByteArrayOutputStream pending = new ByteArrayOutputStream();
        long id_p1 = -1, id_p2 = -1, unused = -1;

        int b, position = 0;
        while (position < 32 && (b = in.read()) != -1) {
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
                case 24: // These bytes are read for consistency reasons.
                case 32: // As the name suggests, they go unused.
                    unused = ByteBuffer.wrap(pending.toByteArray()).order(ByteOrder.BIG_ENDIAN).getLong();
                    pending.reset();
                    break;
            }
        }

        in.close();
        sender.sendPacket(new PacketPingResponse(new UUID(id_p1, id_p2)));
        Long.valueOf(unused);
    }
}
