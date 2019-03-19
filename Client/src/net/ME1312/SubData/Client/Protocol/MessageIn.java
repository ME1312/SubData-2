package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Version.Version;

/**
 * Message In Layout Class
 */
public interface MessageIn {

    /**
     * Receives the incoming Message
     *
     * @throws Throwable
     */
    void receive() throws Throwable;

    /**
     * Protocol Version
     *
     * @return Version
     */
    Version version();

    /**
     * Checks compatibility with an Incoming Message
     *
     * @param version Version of the incoming packet
     * @return Compatibility Status
     */
    default boolean isCompatable(Version version) {
        return Version.equals(version(), version);
    }
}
