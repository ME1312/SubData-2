package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.DataSender;
import net.ME1312.SubData.Client.Library.MessageData;

import org.msgpack.core.MessagePack;

import java.io.OutputStream;

/**
 * Message Object Out Layout Class
 *
 * @param <K> Key Type
 */
public interface MessageObjectOut<K> extends MessageStreamOut {

    /**
     * Sends data within the outgoing Message
     *
     * @param sender The receiving Sender
     * @return Data
     */
    ObjectMap<K> send(DataSender sender) throws Throwable;

    @Override
    default void send(DataSender sender, OutputStream data) throws Throwable {
        ObjectMap<K> output = send(sender);
        if (output != null) MessagePack.newDefaultPacker(data).packValue(MessageData.pack(output)).close();
        else data.close();
    }
}
