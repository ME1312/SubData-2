package net.ME1312.SubData.Server.Protocol;

import java.io.OutputStream;

/**
 * Message OutputStream Layout Class
 */
public interface MessageStreamOut extends MessageOut {

    /**
     * Sends data within the outgoing Message
     *
     * @param data Data Stream
     */
    void send(OutputStream data) throws Throwable;
}
