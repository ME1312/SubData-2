package net.ME1312.SubData.Server.Protocol;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.Library.MessagePackHandler;
import org.msgpack.core.MessagePack;

import java.io.OutputStream;

/**
 * Message Object Out Layout Class
 */
public interface MessageObjectOut extends MessageStreamOut {

    /**
     * Sends data within the outgoing Message
     *
     * @param client Client sending
     * @return Data
     */
    YAMLSection send(DataClient client);

    @Override
    default void send(DataClient client, OutputStream data) throws Throwable {
        YAMLSection output = send(client);
        if (output != null) MessagePack.newDefaultPacker(data).packValue(MessagePackHandler.pack(output)).close();
    }
}
