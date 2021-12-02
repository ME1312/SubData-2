package net.ME1312.SubData.Client.Protocol;

import net.ME1312.SubData.Client.DataSender;

/**
 * Message Out Layout Class
 */
public interface MessageOut {

    /**
     * An Event called when the Message is being sent
     *
     * @param sender The receiving Sender
     */
    default void sending(DataSender sender) throws Throwable {

    }
}
