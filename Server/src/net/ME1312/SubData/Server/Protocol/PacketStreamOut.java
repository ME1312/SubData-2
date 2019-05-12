package net.ME1312.SubData.Server.Protocol;

import net.ME1312.SubData.Server.SubDataClient;

import java.io.OutputStream;

/**
 * Packet OutputStream Layout Class
 */
public interface PacketStreamOut extends PacketOut {

    /**
     * Sends data within the outgoing Packet
     *
     * @param client Client sending
     * @param data Data Stream
     */
    void send(SubDataClient client, OutputStream data) throws Throwable;

    @Override
    default void sending(SubDataClient client) throws Throwable {

    }
}
