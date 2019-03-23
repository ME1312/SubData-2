package net.ME1312.SubData.Client;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.SubData.Client.Protocol.MessageOut;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;

/**
 * SubData Client API Class
 */
public abstract class DataClient {
    private UUID id;

    /**
     * Grabs a Client from the Network
     *
     * @param id Client ID
     * @return Client
     */
    public abstract void getClient(UUID id, Callback<YAMLSection> callback);

    /**
     * Grabs all the Clients on the Network
     *
     * @return Client Map
     */
    public abstract void getClients(Callback<Map<UUID, ? extends YAMLSection>> callback);

    /**
     * Send a message to the Server
     *
     * @param message Message to send
     */
    public abstract void sendMessage(MessageOut message);

    /**
     * Forward a message to another Client
     *
     * @param id Client ID
     * @param message Message to send
     */
    public abstract void forwardMessage(UUID id, MessageOut message);

    /**
     * Get the Protocol for this Client
     *
     * @return Client Protocol
     */
    public abstract DataProtocol getProtocol();

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
