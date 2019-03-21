package net.ME1312.SubData.Server.Protocol;

import net.ME1312.SubData.Server.SubDataClient;

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
    void receive(SubDataClient client, InputStream data) throws Throwable;

    @Override
    default void receive(SubDataClient client) throws Throwable {

    }
}
