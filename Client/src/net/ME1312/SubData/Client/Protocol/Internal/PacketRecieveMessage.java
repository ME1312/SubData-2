package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataProtocol;
import net.ME1312.SubData.Client.Library.Exception.IllegalMessageException;
import net.ME1312.SubData.Client.Library.Exception.IllegalSenderException;
import net.ME1312.SubData.Client.Library.ForwardedDataSender;
import net.ME1312.SubData.Client.Protocol.*;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataSender;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Packet Message Retrieval Class
 */
public class PacketRecieveMessage implements Forwardable, PacketStreamIn {

    @Override
    public void receive(SubDataSender sender, InputStream data) throws Throwable {
        ByteArrayOutputStream pending = new ByteArrayOutputStream();
        String channel = null, handle = null;

        // Parse Message Metadata
        boolean escaped = false;
        int b, state = 0;
        while (state < 2 && (b = data.read()) != -1) {
            if (escaped) {
                switch (b) {
                    case '\u001B': // [ESC] (Escape character)
                        pending.write('\u001B');
                        break;
                    case '\u0003': // [ETX] (End of String character)
                        switch (state) {
                            case 0:
                                channel = new String(pending.toByteArray(), StandardCharsets.UTF_8);
                                break;
                            case 1:
                                handle = new String(pending.toByteArray(), StandardCharsets.UTF_8);
                                break;
                        }
                        pending.reset();
                        state++;
                        break;
                    default:
                        pending.write('\u001B');
                        pending.write(b);
                        break;
                }
                escaped = false;
            } else if (b == '\u001B') {
                escaped = true;
            } else {
                pending.write(b);
            }
        }

        HashMap<String, HashMap<String, MessageIn>> mIn = Util.reflect(DataProtocol.class.getDeclaredField("mIn"), sender.getProtocol());

        if (Util.isNull(channel, handle)) throw new IllegalMessageException("Incomplete Message Metadata: [" + ((channel == null)?"null":"\""+channel+"\"") + ", " + ((handle == null)?"null":"\""+handle+"\"") + "]");
        if (!mIn.keySet().contains(channel) || !mIn.get(channel).keySet().contains(handle)) throw new IllegalMessageException("Could not find handler for message: [\"" + channel + "\", \"" + handle + "\"]");

        MessageIn message = mIn.get(channel).get(handle);
        if (sender instanceof ForwardedDataSender && !(message instanceof Forwardable)) throw new IllegalSenderException("This handler does not support forwarded messages: [" + message.getClass().getTypeName() + "]");
        if (sender instanceof SubDataClient && message instanceof ForwardOnly) throw new IllegalSenderException("This handler does not support non-forwarded messages: [" + message.getClass().getTypeName() + "]");
        message.receive(sender);
        if (message instanceof MessageStreamIn) ((MessageStreamIn) message).receive(sender, data);
        else data.close();
    }
}
