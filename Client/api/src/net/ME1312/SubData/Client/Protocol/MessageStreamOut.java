package net.ME1312.SubData.Client.Protocol;

import net.ME1312.SubData.Client.DataSender;

import java.io.OutputStream;

/**
 * Message OutputStream Layout Class
 */
public interface MessageStreamOut extends MessageOut {

    /**
     * Sends data within the outgoing Message
     *
     * @param sender The receiving Sender
     * @param data Data Stream
     */
    void send(DataSender sender, OutputStream data) throws Throwable;
}
