package net.ME1312.SubData.Server.Protocol.Internal;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.SubData.Server.Library.PingResponse;
import net.ME1312.SubData.Server.Protocol.PacketStreamIn;
import net.ME1312.SubData.Server.Protocol.PacketStreamOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.UUID;

import static net.ME1312.SubData.Server.Protocol.Internal.PacketPing.*;

/**
 * Ping Response Packet
 */
public class PacketPingResponse implements PacketStreamOut, PacketStreamIn {
    private UUID tracker;
    private long init, queue;

    /**
     * New PacketPingResponse (In)
     */
    public PacketPingResponse() {}

    /**
     * New PacketPingResponse (Out)
     *
     * @param tracker UUID Tracker
     */
    public PacketPingResponse(UUID tracker) throws IOException {
        this.tracker = tracker;
        this.init = Calendar.getInstance().getTime().getTime();
    }

    @Override
    public void send(SubDataClient client, OutputStream out) throws Throwable {
        queue = Calendar.getInstance().getTime().getTime();

        out.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(tracker.getMostSignificantBits()).array());
        out.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(tracker.getLeastSignificantBits()).array());
        out.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(init).array());  // Object Initialization (Remote) [2]
        out.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(queue).array()); // On Send (Remote) [3]
        out.close();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataClient client, InputStream in) throws Throwable {
        ByteArrayOutputStream pending = new ByteArrayOutputStream();
        long id_p1 = -1, id_p2 = -1;
        long[] timings = new long[5];

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
                case 24:
                    timings[2] = ByteBuffer.wrap(pending.toByteArray()).order(ByteOrder.BIG_ENDIAN).getLong();
                    pending.reset();
                    break;
                case 32:
                    timings[3] = ByteBuffer.wrap(pending.toByteArray()).order(ByteOrder.BIG_ENDIAN).getLong();
                    pending.reset();
                    break;
            }
        }

        UUID id = new UUID(id_p1, id_p2);
        if (data.keySet().contains(id)) {
            timings[4] = Calendar.getInstance().getTime().getTime(); // Transaction Complete [4]

            timings[0] = data.get(id).init;
            timings[1] = data.get(id).queue;

            for (Callback<PingResponse> callback : callbacks.get(id)) callback.run(new PingResponse(timings));
            callbacks.remove(id);
            data.remove(id);
        }
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
