package net.ME1312.SubData.Server.Protocol;

import net.ME1312.SubData.Server.Client;

import java.io.InputStream;

/**
 * Message InputStream Layout Class
 */
public interface MessageStreamIn extends MessageIn {

    /**
     * Receives the incoming Message
     *
     * @param client Client who sent
     * @param data Data Stream
     * @throws Throwable
     */
    void receive(Client client, InputStream data) throws Throwable;

    @Override
    default void receive(Client client) throws Throwable {

    }
}
