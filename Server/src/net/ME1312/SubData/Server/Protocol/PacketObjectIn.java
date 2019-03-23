package net.ME1312.SubData.Server.Protocol;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.SubData.Server.Library.MessagePackHandler;
import net.ME1312.SubData.Server.SubDataClient;
import org.msgpack.core.MessagePack;

import java.io.InputStream;

/**
 * Packet Object In Layout Class
 */
public interface PacketObjectIn extends PacketStreamIn {

    /**
     * Receives the incoming Packet
     *
     * @param client Client who sent
     * @param data Data Object
     * @throws Throwable
     */
    void receive(SubDataClient client, YAMLSection data) throws Throwable;

    @Override
    default void receive(SubDataClient client, InputStream data) throws Throwable {
        receive(client, MessagePackHandler.unpack(MessagePack.newDefaultUnpacker(data).unpackValue().asMapValue()));
    }
}
