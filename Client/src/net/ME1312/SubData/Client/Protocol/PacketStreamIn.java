package net.ME1312.SubData.Client.Protocol;

import java.io.InputStream;

/**
 * Packet InputStream Layout Class
 */
public interface PacketStreamIn extends PacketIn {

    /**
     * Receives the incoming Packet
     *
     * @param data Data Stream
     * @throws Throwable
     */
    void receive(InputStream data) throws Throwable;

    @Override
    default void receive() throws Throwable {

    }
}
