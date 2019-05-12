package net.ME1312.SubData.Client.Protocol;

import net.ME1312.SubData.Client.SubDataClient;

/**
 * Packet Out Layout Class
 */
public interface PacketOut {

    /**
     * An Event called when the Packet is being sent
     *
     * @param client Client sending
     */
    default void sending(SubDataClient client) throws Throwable {

    }

    /**
     * Protocol Version
     *
     * @return Version (as an unsigned 16-bit value)
     */
    int version();
}
