package net.ME1312.SubData.Server.Protocol;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.SubData.Server.Client;
import net.ME1312.SubData.Server.Library.MessagePackHandler;
import org.msgpack.core.MessagePack;

import java.io.InputStream;

/**
 * Message Object In Layout Class
 */
public interface MessageObjectIn extends MessageStreamIn {

    /**
     * Receives the incoming Message
     *
     * @param client Client who sent
     * @param data Data Object
     * @throws Throwable
     */
    void receive(Client client, YAMLSection data);

    @Override
    default void receive(Client client, InputStream data) throws Throwable {
        receive(client, MessagePackHandler.unpack(MessagePack.newDefaultUnpacker(data).unpackValue().asMapValue()));
    }
}
