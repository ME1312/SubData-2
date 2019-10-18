package net.ME1312.SubData.Client.Protocol;

import net.ME1312.SubData.Client.SubDataSender;

/**
 * Packet Out Layout Class
 */
public interface PacketOut {

    /**
     * An Event called when the Packet is being sent
     *
     * @param sender The receiving Sender
     */
    default void sending(SubDataSender sender) throws Throwable {

    }

    /**
     * Protocol Version
     *
     * @return Version (as an unsigned 16-bit value)
     */
    int version();
}
