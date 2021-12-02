package net.ME1312.SubData.Server.Protocol;

import net.ME1312.SubData.Server.DataClient;

/**
 * Message In Layout Class
 */
public interface MessageIn {

    /**
     * Receives the incoming Message
     *
     * @param client Client who sent
     * @throws Throwable
     */
    void receive(DataClient client) throws Throwable;
}
