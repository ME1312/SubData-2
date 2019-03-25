package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Library.MessagePackHandler;
import net.ME1312.SubData.Client.SubDataClient;
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
     * @param client Client sending
     * @return Data
     */
    ObjectMap<K> send(SubDataClient client) throws Throwable;

    @Override
    default void send(SubDataClient client, OutputStream data) throws Throwable {
        ObjectMap<K> output = send(client);
        if (output == null) output = new ObjectMap<K>();
        MessagePack.newDefaultPacker(data).packValue(MessagePackHandler.pack(output)).close();
    }
}
