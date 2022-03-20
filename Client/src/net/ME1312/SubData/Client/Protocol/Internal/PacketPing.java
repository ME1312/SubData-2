package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.PingResponse;
import net.ME1312.SubData.Client.Protocol.Forwardable;
import net.ME1312.SubData.Client.Protocol.PacketStreamIn;
import net.ME1312.SubData.Client.Protocol.PacketStreamOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Ping Packet
 */
public class PacketPing implements Forwardable, PacketStreamOut, PacketStreamIn {
    static HashMap<UUID, PacketPing> requests = new HashMap<UUID, PacketPing>();
    Consumer<PingResponse>[] callbacks;
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
        requests.put(tracker = Util.getNew(requests.keySet(), UUID::randomUUID), this);
        callbacks = callback;
        init = System.nanoTime();
    }

    @Override
    public void send(SubDataSender sender, OutputStream out) throws Throwable {
        queue = System.nanoTime();

        out.write(ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN)
                .putLong(tracker.getMostSignificantBits())
                .putLong(tracker.getLeastSignificantBits())
                .putLong(init)  // Object Initialization (Local) [0]
                .putLong(queue) // On Send (Local) [1]
                .array()
        );
        out.close();
    }

    @Override
    public void receive(SubDataSender sender, InputStream in) throws Throwable {
        ByteBuffer data = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);

        int b, i = 0;
        while ((b = in.read()) != -1) {
            data.put((byte) b);
            if (++i == 32) break;
        }
        in.close();
        data.position(0);

        UUID request = new UUID(data.getLong(), data.getLong());
        data.getLong(); // These bytes are read for timing consistency.
        data.getLong(); // As the code suggests, they go unused.
        sender.sendPacket(new PacketPingResponse(request, System.nanoTime()));
    }
}
