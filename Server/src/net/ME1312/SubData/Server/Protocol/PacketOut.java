package net.ME1312.SubData.Server.Protocol;

import net.ME1312.SubData.Server.SubDataClient;

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
}
