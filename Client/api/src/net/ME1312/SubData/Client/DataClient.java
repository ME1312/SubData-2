package net.ME1312.SubData.Client;

import net.ME1312.SubData.Client.Protocol.MessageOut;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * SubData Client API Class
 */
public abstract class DataClient {

    /**
     * Send Message to Client
     *
     * @param message Packet to send
     */
    public abstract void sendMessage(MessageOut message);

    /**
     * Get the Protocol for this Client
     *
     * @return Client Protocol
     */
    public abstract DataProtocol getProtocol();

    /**
     * Get Remote Address
     *
     * @return Address
     */
    public abstract InetSocketAddress getAddress();

    /**
     * Closes the connection
     *
     * @throws IOException
     */
    public abstract void close() throws IOException;

    /**
     * Get if the connection has been closed
     *
     * @return Closed Status
     */
    public abstract boolean isClosed();
}
