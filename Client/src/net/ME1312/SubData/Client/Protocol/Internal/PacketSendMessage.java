package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataProtocol;
import net.ME1312.SubData.Client.Library.Exception.IllegalMessageException;
import net.ME1312.SubData.Client.Protocol.Forwardable;
import net.ME1312.SubData.Client.Protocol.MessageOut;
import net.ME1312.SubData.Client.Protocol.MessageStreamOut;
import net.ME1312.SubData.Client.Protocol.PacketStreamOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Packet Message Sending Class
 */
public class PacketSendMessage implements Forwardable, PacketStreamOut {
    private MessageOut message;

    /**
     * PacketSendMessage (Out)
     *
     * @param message Message to send
     */
    public PacketSendMessage(MessageOut message) {
        this.message = message;
    }

    @Override
    public void send(SubDataSender sender, OutputStream data) throws Throwable {
        HashMap<Class<? extends MessageOut>, NamedContainer<String, String>> mOut = Util.reflect(DataProtocol.class.getDeclaredField("mOut"), sender.getProtocol());

        if (!mOut.keySet().contains(message.getClass())) throw new IllegalMessageException("Could not find handle for message: " + message.getClass().getCanonicalName());
        if (message.version() == null || message.version().toString().length() == 0) throw new IllegalMessageException("Cannot send message with null version: " + message.getClass().getCanonicalName());

        data.write(escape(mOut.get(message.getClass()).name().getBytes(StandardCharsets.UTF_8)));
        data.write('\u0003');
        data.write(escape(mOut.get(message.getClass()).get().getBytes(StandardCharsets.UTF_8)));
        data.write('\u0003');
        data.write(escape(message.version().toString().getBytes(StandardCharsets.UTF_8)));
        data.write('\u0003');
        if (message instanceof MessageStreamOut) ((MessageStreamOut) message).send(sender, data);
        else data.close();
    }

    private byte[] escape(byte[] array) {
        ByteArrayInputStream in = new ByteArrayInputStream(array);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int b;
        while ((b = in.read()) != -1) {
            switch (b) {
                case '\u0002': // [STX] (Escape character)
                case '\u0003': // [ETX] (State change character)
                    out.write('\u0002');
                default:
                    out.write(b);
                    break;
            }
        }

        return out.toByteArray();
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
