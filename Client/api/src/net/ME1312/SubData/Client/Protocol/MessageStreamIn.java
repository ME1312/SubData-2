package net.ME1312.SubData.Client.Protocol;

import net.ME1312.SubData.Client.DataSender;

import java.io.InputStream;

/**
 * Message InputStream Layout Class
 */
public interface MessageStreamIn extends MessageIn {

    /**
     * Receives the incoming Message
     *
     * @param sender Sender who sent
     * @param data Data Stream
     * @throws Throwable
     */
    void receive(DataSender sender, InputStream data) throws Throwable;

    @Override
    default void receive(DataSender sender) throws Throwable {

    }
}
