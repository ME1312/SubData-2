package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.DataProtocol;
import net.ME1312.SubData.Client.Library.Exception.IllegalMessageException;
import net.ME1312.SubData.Client.Protocol.MessageIn;
import net.ME1312.SubData.Client.Protocol.MessageStreamIn;
import net.ME1312.SubData.Client.Protocol.PacketStreamIn;
import net.ME1312.SubData.Client.SubDataClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Packet Message Retrieval Class
 */
public class PacketRecieveMessage implements PacketStreamIn {

    @Override
    public void receive(SubDataClient client, InputStream data) throws Throwable {
        ByteArrayOutputStream pending = new ByteArrayOutputStream();
        String channel = null, handle = null;
        Version version = null;

        // Parse Message Metadata
        boolean escaped = false;
        int b, state = 0;
        while (state < 3 && (b = data.read()) != -1) {
            if (escaped) {
                pending.write(b);
                escaped = false;
            } else if (state < 3) switch (b) {
                case '\u0002': // [STX] (Escape character)
                    escaped = true;
                    break;
                case '\u0003': // [ETX] (State change character)
                    switch (state) {
                        case 0:
                            channel = new String(pending.toByteArray(), StandardCharsets.UTF_8);
                            break;
                        case 1:
                            handle = new String(pending.toByteArray(), StandardCharsets.UTF_8);
                            break;
                        case 2:
                            version = Version.fromString(new String(pending.toByteArray(), StandardCharsets.UTF_8));
                            break;
                    }
                    pending.reset();
                    state++;
                    break;
                default:
                    pending.write(b);
                    break;
            }
        }

        HashMap<String, HashMap<String, MessageIn>> mIn = Util.reflect(DataProtocol.class.getDeclaredField("mIn"), client.getProtocol());

        if (Util.isNull(channel, handle, version)) throw new IllegalMessageException("Incomplete Message Metadata: [" + channel + ", " + handle + ", " + version + "]");
        if (!mIn.keySet().contains(channel) || !mIn.get(channel).keySet().contains(handle)) throw new IllegalMessageException("Could not find handler for message: [" + channel + ", " + handle + ", " + version + "]");

        MessageIn message = mIn.get(channel).get(handle);
        if (!message.isCompatable(version)) throw new IllegalMessageException("The handler does not support this message version (" + message.version() + "): [" + channel + ", " + handle + ", " + version + "]");
        message.receive(client);
        if (message instanceof MessageStreamIn) ((MessageStreamIn) message).receive(client, data);
        else data.close();
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
