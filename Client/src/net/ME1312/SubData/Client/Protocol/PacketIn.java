package net.ME1312.SubData.Client.Protocol;

import net.ME1312.SubData.Client.SubDataSender;

/**
 * Packet In Layout Class
 */
public interface PacketIn {

    /**
     * Receives the incoming Packet
     *
     * @param sender Sender who sent
     * @throws Throwable
     */
    void receive(SubDataSender sender) throws Throwable;
}
