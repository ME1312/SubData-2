package net.ME1312.SubData.Server.Protocol;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.Library.MessagePackHandler;
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
     * @param client Client sending
     * @return Data
     */
    ObjectMap<K> send(DataClient client) throws Throwable;

    @Override
    default void send(DataClient client, OutputStream data) throws Throwable {
        ObjectMap<K> output = send(client);
        if (output == null) output = new ObjectMap<K>();
        MessagePack.newDefaultPacker(data).packValue(MessagePackHandler.pack(output)).close();
    }
}
