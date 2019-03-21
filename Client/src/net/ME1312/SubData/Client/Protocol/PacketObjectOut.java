package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.SubData.Client.Library.MessagePackHandler;
import net.ME1312.SubData.Client.SubDataClient;
import org.msgpack.core.MessagePack;

import java.io.OutputStream;

/**
 * Packet Object Out Layout Class
 */
public interface PacketObjectOut extends PacketStreamOut {

    /**
     * Sends data within the outgoing Packet
     *
     * @param client Client sending
     * @return Data
     */
    YAMLSection send(SubDataClient client);

    @Override
    default void send(SubDataClient client, OutputStream data) throws Throwable {
        YAMLSection output = send(client);
        if (output != null) MessagePack.newDefaultPacker(data).packValue(MessagePackHandler.pack(output)).close();
    }
}
