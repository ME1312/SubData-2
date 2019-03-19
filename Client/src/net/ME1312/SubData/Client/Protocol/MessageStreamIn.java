package net.ME1312.SubData.Client.Protocol;

import java.io.InputStream;

/**
 * Message InputStream Layout Class
 */
public interface MessageStreamIn extends MessageIn {

    /**
     * Receives the incoming Message
     *
     * @param data Data Stream
     * @throws Throwable
     */
    void receive(InputStream data) throws Throwable;

    @Override
    default void receive() throws Throwable {

    }
}
