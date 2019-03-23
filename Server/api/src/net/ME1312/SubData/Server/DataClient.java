package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * SubData Client API Class
 */
public abstract class DataClient {
    private UUID id;

    DataClient(UUID id) {
        if (Util.isNull(id)) throw new NullPointerException();
        this.id = id;
    }

    /**
     * Send a message to the Client
     *
     * @param message Message to send
     */
    public abstract void sendMessage(MessageOut message);

    /**
     * Get the Server this Client belongs to
     *
     * @return SubData Server
     */
    public abstract DataServer getServer();

    /**
     * Get the ID of this Client
     *
     * @return Client ID
     */
    public UUID getID() {
        return id;
    }

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
     * Wait for the connection to close
     *
     * @throws InterruptedException
     */
    public void waitFor() throws InterruptedException {
        while (!isClosed()) Thread.sleep(125);
    }

    /**
     * Get if the connection has been closed
     *
     * @return Closed Status
     */
    public abstract boolean isClosed();
}
