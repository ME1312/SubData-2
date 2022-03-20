package net.ME1312.SubData.Server.Protocol.Internal;

import net.ME1312.SubData.Server.Library.PingResponse;
import net.ME1312.SubData.Server.Protocol.PacketStreamIn;
import net.ME1312.SubData.Server.Protocol.PacketStreamOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import java.util.function.Consumer;

import static net.ME1312.SubData.Server.Protocol.Internal.PacketPing.requests;

/**
 * Ping Response Packet
 */
public class PacketPingResponse implements PacketStreamOut, PacketStreamIn {
    private UUID tracker;
    private long init;

    /**
     * New PacketPingResponse (In)
     */
    public PacketPingResponse() {}

    /**
     * New PacketPingResponse (Out)
     *
     * @param tracker UUID Tracker
     * @param time Nanosecond Timing
     */
    public PacketPingResponse(UUID tracker, long time) throws IOException {
        this.tracker = tracker;
        this.init = time;
    }

    @Override
    public void send(SubDataClient client, OutputStream out) throws Throwable {
        long queue = System.nanoTime();

        out.write(ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN)
                .putLong(tracker.getMostSignificantBits())
                .putLong(tracker.getLeastSignificantBits())
                .putLong(init)  // Object Initialization (Remote) [2]
                .putLong(queue) // On Send (Remote) [3]
                .array()
        );
        out.close();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, InputStream in) throws Throwable {
        ByteBuffer data = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        long[] timings = new long[5];

        int b, i = 0;
        while ((b = in.read()) != -1) {
            data.put((byte) b);
            if (++i == 32) break;
        }
        in.close();
        data.position(0);

        UUID id = new UUID(data.getLong(), data.getLong());
        timings[2] = data.getLong();
        timings[3] = data.getLong();
        timings[4] = System.nanoTime(); // Transaction Complete [4]

        PacketPing request = requests.remove(id);
        if (request != null) {
            timings[0] = request.init;
            timings[1] = request.queue;

            for (Consumer<PingResponse> callback : request.callbacks) callback.accept(new PingResponse(timings));
        }
    }
}
