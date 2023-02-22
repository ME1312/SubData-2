package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.DataProtocol;
import net.ME1312.SubData.Client.Library.EscapedOutputStream;
import net.ME1312.SubData.Client.Library.Exception.IllegalMessageException;
import net.ME1312.SubData.Client.Protocol.Forwardable;
import net.ME1312.SubData.Client.Protocol.MessageOut;
import net.ME1312.SubData.Client.Protocol.MessageStreamOut;
import net.ME1312.SubData.Client.Protocol.PacketStreamOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Packet Message Sending Class
 */
public class PacketSendMessage implements Forwardable, PacketStreamOut {
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
    public void send(SubDataSender sender, OutputStream data) throws Throwable {
        HashMap<Class<? extends MessageOut>, Pair<String, String>> mOut = Util.reflect(DataProtocol.class.getDeclaredField("mOut"), sender.getProtocol());

        if (!mOut.containsKey(message.getClass())) throw new IllegalMessageException("Could not find handle for message: " + message.getClass().getTypeName());

        EscapedOutputStream out = new EscapedOutputStream(data, '\u001B', '\u0003');
        out.write(mOut.get(message.getClass()).key().getBytes(StandardCharsets.UTF_8));
        out.control('\u0003');
        out.write(mOut.get(message.getClass()).value().getBytes(StandardCharsets.UTF_8));
        out.control('\u0003');
        if (message instanceof MessageStreamOut) ((MessageStreamOut) message).send(sender, data);
        else data.close();
    }

    @Override
    public void sending(SubDataSender sender) throws Throwable {
        message.sending(sender);
    }
}
