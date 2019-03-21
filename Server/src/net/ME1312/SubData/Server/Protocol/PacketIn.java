package net.ME1312.SubData.Server.Protocol;

import net.ME1312.SubData.Server.SubDataClient;

/**
 * Packet In Layout Class
 */
public interface PacketIn {

    /**
     * Receives the incoming Packet
     *
     * @param client Client who sent
     * @throws Throwable
     */
    void receive(SubDataClient client) throws Throwable;

    /**
     * Protocol Version
     *
     * @return Version (as an unsigned 16-bit value)
     */
    int version();

    /**
     * Checks compatibility with an Incoming Packet
     *
     * @param version Version of the incoming packet
     * @return Compatibility Status
     */
    default boolean isCompatable(int version) {
        return version() == version;
    }
}
