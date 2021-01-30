package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Library.MessageData;
import net.ME1312.SubData.Client.SubDataSender;

import org.msgpack.core.MessagePack;

import java.io.OutputStream;

/**
 * Packet Object Out Layout Class
 *
 * @param <K> Key Type
 */
public interface PacketObjectOut<K> extends PacketStreamOut {

    /**
     * Sends data within the outgoing Packet
     *
     * @param sender The receiving Sender
     * @return Data
     */
    ObjectMap<K> send(SubDataSender sender) throws Throwable;

    @Override
    default void send(SubDataSender sender, OutputStream data) throws Throwable {
        ObjectMap<K> output = send(sender);
        if (output != null) MessagePack.newDefaultPacker(data).packValue(MessageData.pack(output)).close();
        else data.close();
    }
}
