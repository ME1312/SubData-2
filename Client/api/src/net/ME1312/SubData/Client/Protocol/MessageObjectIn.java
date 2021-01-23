package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.DataSender;
import net.ME1312.SubData.Client.Library.MessagePackHandler;

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
     * @param sender Sender who sent
     * @param data Data Object
     * @throws Throwable
     */
    void receive(DataSender sender, ObjectMap<K> data) throws Throwable;

    @Override
    default void receive(DataSender sender, InputStream data) throws Throwable {
        try (MessageUnpacker msg = MessagePack.newDefaultUnpacker(data)) {
            receive(sender, MessagePackHandler.unpack(msg.unpackValue().asMapValue()));
        } catch (MessageInsufficientBufferException e) {
            receive(sender, (ObjectMap<K>) null);
        }
    }
}
