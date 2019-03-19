package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.SubData.Client.Library.MessagePackHandler;
import org.msgpack.core.MessagePack;

import java.io.InputStream;

/**
 * Packet Object In Layout Class
 */
public interface PacketObjectIn extends PacketStreamIn {

    /**
     * Receives the incoming Packet
     *
     * @param data Data Object
     * @throws Throwable
     */
    void receive(YAMLSection data);

    @Override
    default void receive(InputStream data) throws Throwable {
        receive(MessagePackHandler.unpack(MessagePack.newDefaultUnpacker(data).unpackValue().asMapValue()));
    }
}
