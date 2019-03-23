package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Library.MessagePackHandler;
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
    void receive(DataClient client, YAMLSection data) throws Throwable;

    @Override
    default void receive(DataClient client, InputStream data) throws Throwable {
        receive(client, MessagePackHandler.unpack(MessagePack.newDefaultUnpacker(data).unpackValue().asMapValue()));
    }
}
