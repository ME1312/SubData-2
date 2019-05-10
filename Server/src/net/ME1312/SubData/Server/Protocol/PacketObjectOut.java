package net.ME1312.SubData.Server.Protocol;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Server.Library.MessagePackHandler;
import net.ME1312.SubData.Server.SubDataClient;
import org.msgpack.core.MessagePack;

import java.io.OutputStream;

/**
 * Packet Object Out Layout Class
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
        if (output != null) MessagePack.newDefaultPacker(data).packValue(MessagePackHandler.pack(output)).close();
        else data.close();
    }
}
