package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.SubData.Client.DataClient;
import net.ME1312.SubData.Client.Library.MessagePackHandler;
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
    YAMLSection send(DataClient client) throws Throwable;

    @Override
    default void send(DataClient client, OutputStream data) throws Throwable {
        YAMLSection output = send(client);
        if (output == null) output = new YAMLSection();
        MessagePack.newDefaultPacker(data).packValue(MessagePackHandler.pack(output)).close();
    }
}
