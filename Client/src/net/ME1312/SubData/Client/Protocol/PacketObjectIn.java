package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Library.MessageData;
import net.ME1312.SubData.Client.SubDataSender;

import org.msgpack.core.MessageInsufficientBufferException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

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
     * @param sender Sender who sent
     * @param data Data Object
     * @throws Throwable
     */
    void receive(SubDataSender sender, ObjectMap<K> data) throws Throwable;

    @Override
    default void receive(SubDataSender sender, InputStream data) throws Throwable {
        try (MessageUnpacker msg = MessagePack.newDefaultUnpacker(data)) {
            receive(sender, MessageData.unpack(msg.unpackValue().asMapValue()));
        } catch (MessageInsufficientBufferException e) {
            receive(sender, (ObjectMap<K>) null);
        }
    }
}
