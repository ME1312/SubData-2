package net.ME1312.SubData.Client.Protocol;

import net.ME1312.SubData.Client.SubDataClient;

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
}
