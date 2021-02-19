package net.ME1312.SubData.Server.Protocol.Internal;

import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.DataProtocol;
import net.ME1312.SubData.Server.Library.EscapedOutputStream;
import net.ME1312.SubData.Server.Library.Exception.IllegalMessageException;
import net.ME1312.SubData.Server.Protocol.MessageOut;
import net.ME1312.SubData.Server.Protocol.MessageStreamOut;
import net.ME1312.SubData.Server.Protocol.PacketStreamOut;
import net.ME1312.SubData.Server.SubDataClient;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Packet Message Sending Class
 */
public class PacketSendMessage implements PacketStreamOut {
    private final MessageOut message;

    /**
     * PacketSendMessage (Out)
     *
     * @param message Message to send
     */
    public PacketSendMessage(MessageOut message) {
        this.message = message;
    }

    @Override
    public void send(SubDataClient client, OutputStream data) throws Throwable {
        HashMap<Class<? extends MessageOut>, Pair<String, String>> mOut = Util.reflect(DataProtocol.class.getDeclaredField("mOut"), client.getServer().getProtocol());

        if (!mOut.keySet().contains(message.getClass())) throw new IllegalMessageException("Could not find handle for message: " + message.getClass().getCanonicalName());
        if (message.version() == null || message.version().toString().length() == 0) throw new IllegalMessageException("Cannot send message with null version: " + message.getClass().getCanonicalName());

        EscapedOutputStream out = new EscapedOutputStream(data, '\u001B', '\u0003');
        out.write(mOut.get(message.getClass()).key().getBytes(StandardCharsets.UTF_8));
        out.control('\u0003');
        out.write(mOut.get(message.getClass()).value().getBytes(StandardCharsets.UTF_8));
        out.control('\u0003');
        out.write(message.version().toFullString().getBytes(StandardCharsets.UTF_8));
        out.control('\u0003');
        if (message instanceof MessageStreamOut) ((MessageStreamOut) message).send(client, data);
        else data.close();
    }

    @Override
    public void sending(SubDataClient client) throws Throwable {
        message.sending(client);
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
