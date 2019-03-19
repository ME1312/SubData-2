package net.ME1312.SubData.Server.Protocol;

import net.ME1312.SubData.Server.Client;

import java.io.InputStream;

/**
 * Packet InputStream Layout Class
 */
public interface PacketStreamIn extends PacketIn {

    /**
     * Receives the incoming Packet
     *
     * @param client Client who sent
     * @param data Data Stream
     * @throws Throwable
     */
    void receive(Client client, InputStream data) throws Throwable;

    @Override
    default void receive(Client client) throws Throwable {

    }
}
