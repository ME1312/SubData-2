package net.ME1312.SubData.Server.Protocol;

import net.ME1312.SubData.Server.DataClient;

import java.io.OutputStream;

/**
 * Message OutputStream Layout Class
 */
public interface MessageStreamOut extends MessageOut {

    /**
     * Sends data within the outgoing Message
     *
     * @param client Client sending
     * @param data Data Stream
     */
    void send(DataClient client, OutputStream data) throws Throwable;

    @Override
    default void sending(DataClient client) throws Throwable {

    }
}
