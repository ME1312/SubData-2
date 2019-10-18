package net.ME1312.SubData.Client.Protocol;

import net.ME1312.SubData.Client.SubDataSender;

import java.io.InputStream;

/**
 * Packet InputStream Layout Class
 */
public interface PacketStreamIn extends PacketIn {

    /**
     * Receives the incoming Packet
     *
     * @param sender Sender who sent
     * @param data Data Stream
     * @throws Throwable
     */
    void receive(SubDataSender sender, InputStream data) throws Throwable;

    @Override
    default void receive(SubDataSender sender) throws Throwable {

    }
}
