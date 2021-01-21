package net.ME1312.SubData.Server.Protocol;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.Library.MessagePackHandler;

import org.msgpack.core.MessageInsufficientBufferException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.InputStream;

/**
 * Message Object In Layout Class
 *
 * @param <K> Key Type
 */
public interface MessageObjectIn<K> extends MessageStreamIn {

    /**
     * Receives the incoming Message
     *
     * @param client Client who sent
     * @param data Data Object
     * @throws Throwable
     */
    void receive(DataClient client, ObjectMap<K> data) throws Throwable;

    @Override
    default void receive(DataClient client, InputStream data) throws Throwable {
        try (MessageUnpacker msg = MessagePack.newDefaultUnpacker(data)) {
            receive(client, MessagePackHandler.unpack(msg.unpackValue().asMapValue()));
        } catch (MessageInsufficientBufferException e) {
            data.close();
            receive(client, (ObjectMap<K>) null);
        }
    }
}
