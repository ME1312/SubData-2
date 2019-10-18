package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Version.Version;
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
    default boolean isCompatible(Version version) {
        return Version.equals(version(), version);
    }
}
