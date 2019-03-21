package net.ME1312.SubData.Server;

import net.ME1312.SubData.Server.Protocol.*;

import java.io.*;
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
     * Get the Server this Client belongs to
     *
     * @return SubData Server
     */
    public abstract DataServer getServer();

    /**
     * Get Remote Address
     *
     * @return Address
     */
    public abstract InetSocketAddress getAddress();

    /**
     * Gets the Linked Handler
     *
     * @return Handler
     */
    public abstract ClientHandler getHandler();

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
