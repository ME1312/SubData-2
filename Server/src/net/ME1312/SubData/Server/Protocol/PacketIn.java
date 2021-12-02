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
}
