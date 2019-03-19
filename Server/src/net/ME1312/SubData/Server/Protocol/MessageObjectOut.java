package net.ME1312.SubData.Server.Protocol;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
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
     * @return Data
     */
    YAMLSection send();

    @Override
    default void send(OutputStream data) throws Throwable {
        YAMLSection output = send();
        if (output != null) MessagePack.newDefaultPacker(data).packValue(MessagePackHandler.pack(output)).close();
    }
}
