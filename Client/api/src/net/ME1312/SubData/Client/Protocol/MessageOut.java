package net.ME1312.SubData.Client.Protocol;

import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.DataClient;

/**
 * Message Out Layout Class
 */
public interface MessageOut {

    /**
     * An Event called when the Message is being sent
     *
     * @param client Client sending
     */
    void sending(DataClient client) throws Throwable;

    /**
     * Protocol Version
     *
     * @return Version
     */
    Version version();
}
