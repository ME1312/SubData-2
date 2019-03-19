package net.ME1312.SubData.Client.Protocol;

import java.io.OutputStream;

/**
 * Packet OutputStream Layout Class
 */
public interface PacketStreamOut extends PacketOut {

    /**
     * Sends data within the outgoing Packet
     *
     * @param data Data Stream
     */
    void send(OutputStream data) throws Throwable;
}
