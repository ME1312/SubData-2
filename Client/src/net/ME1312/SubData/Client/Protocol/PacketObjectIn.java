package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Library.MessagePackHandler;
import net.ME1312.SubData.Client.SubDataClient;
import org.msgpack.core.MessageInsufficientBufferException;
import org.msgpack.core.MessagePack;

import java.io.InputStream;

/**
 * Packet Object In Layout Class
 *
 * @param <K> Key Type
 */
public interface PacketObjectIn<K> extends PacketStreamIn {

    /**
     * Receives the incoming Packet
     *
     * @param client Client who sent
     * @param data Data Object
     * @throws Throwable
     */
    void receive(SubDataClient client, ObjectMap<K> data) throws Throwable;

    @Override
    default void receive(SubDataClient client, InputStream data) throws Throwable {
        try {
            receive(client, MessagePackHandler.unpack(MessagePack.newDefaultUnpacker(data).unpackValue().asMapValue()));
        } catch (MessageInsufficientBufferException e) {
            receive(client, (ObjectMap<K>) null);
            data.close();
        }
    }
}
