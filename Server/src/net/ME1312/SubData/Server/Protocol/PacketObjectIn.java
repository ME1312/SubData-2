package net.ME1312.SubData.Server.Protocol;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Server.Library.MessagePackHandler;
import net.ME1312.SubData.Server.SubDataClient;

import org.msgpack.core.MessageInsufficientBufferException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.InputStream;

/**
 * Packet Object In Layout Class
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
        try (MessageUnpacker msg = MessagePack.newDefaultUnpacker(data)) {
            receive(client, MessagePackHandler.unpack(msg.unpackValue().asMapValue()));
        } catch (MessageInsufficientBufferException e) {
            data.close();
            receive(client, (ObjectMap<K>) null);
        }
    }
}
