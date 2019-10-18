package net.ME1312.SubData.Client.Protocol;

import net.ME1312.SubData.Client.SubDataSender;

import java.io.OutputStream;

/**
 * Packet OutputStream Layout Class
 */
public interface PacketStreamOut extends PacketOut {

    /**
     * Sends data within the outgoing Packet
     *
     * @param sender The receiving Sender
     * @param data Data Stream
     */
    void send(SubDataSender sender, OutputStream data) throws Throwable;
}
