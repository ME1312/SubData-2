package net.ME1312.SubData.Client.Protocol;

import net.ME1312.SubData.Client.DataSender;

/**
 * Message In Layout Class
 */
public interface MessageIn {

    /**
     * Receives the incoming Message
     *
     * @param sender Sender who sent
     * @throws Throwable
     */
    void receive(DataSender sender) throws Throwable;
}
