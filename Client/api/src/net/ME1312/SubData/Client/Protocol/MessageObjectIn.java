package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Library.MessagePackHandler;
import org.msgpack.core.MessagePack;

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
        receive(client, MessagePackHandler.unpack(MessagePack.newDefaultUnpacker(data).unpackValue().asMapValue()));
    }
}
