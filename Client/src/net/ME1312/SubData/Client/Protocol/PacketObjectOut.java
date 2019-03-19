package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.SubData.Client.Library.MessagePackHandler;
import org.msgpack.core.MessagePack;

import java.io.OutputStream;

/**
 * Packet Object Out Layout Class
 */
public interface PacketObjectOut extends PacketStreamOut {

    /**
     * Sends data within the outgoing Packet
     *
     * @return Data
     */
    YAMLSection send();

    @Override
    default void send(OutputStream data) throws Throwable {
        YAMLSection output = send();
        if (output != null) MessagePack.newDefaultPacker(data).packValue(MessagePackHandler.pack(output)).close();
    }
}
