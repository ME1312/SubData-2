package net.ME1312.SubData.Client;

import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.SubData.Client.Library.DisconnectReason;
import net.ME1312.SubData.Client.Library.PingResponse;
import net.ME1312.SubData.Client.Protocol.MessageOut;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * SubData Client API Class
 */
public abstract class DataClient implements DataSender {
    public final Events on = new Events();
    private UUID id;

    /**
     * SubData Client Event API Class
     */
    public static class Events {
        LinkedList<Consumer<DataClient>> ready = new LinkedList<Consumer<DataClient>>();
        LinkedList<Function<DataClient, Boolean>> close = new LinkedList<Function<DataClient, Boolean>>();
        LinkedList<Consumer<Pair<DisconnectReason, DataClient>>> closed = new LinkedList<Consumer<Pair<DisconnectReason, DataClient>>>();
        private Events() {}

        /**
         * On Connection Ready Event
         *
         * @param callbacks Callback
         */
        @SafeVarargs
        public final void ready(Consumer<DataClient>... callbacks) {
            ready.addAll(Arrays.asList(callbacks));
        }

        /**
         * On Connection Close Event
         *
         * @param callbacks Callback
         */
        @SafeVarargs
        public final void close(Function<DataClient, Boolean>... callbacks) {
            close.addAll(Arrays.asList(callbacks));
        }

        /**
         * On Connection Closed Event
         *
         * @param callbacks Callback
         */
        @SafeVarargs
        public final void closed(Consumer<Pair<DisconnectReason, DataClient>>... callbacks) {
            closed.addAll(Arrays.asList(callbacks));
        }
    }

    /**
     * Grabs a Client from the Network
     *
     * @param id Client ID
     * @return Client
     */
    public abstract void getClient(UUID id, Consumer<ObjectMap<String>> callback);

    /**
     * Grabs all the Clients on the Network
     *
     * @return Client Map
     */
    public abstract void getClients(Consumer<Map<UUID, ObjectMap<String>>> callback);

    /**
     * Ping the Server
     *
     * @param response Ping Response
     */
    public abstract void ping(Consumer<PingResponse> response);

    /**
     * Ping a remote Client
     *
     * @param id Client ID
     * @param response Ping Response
     */
    public abstract void ping(UUID id, Consumer<PingResponse> response);

    /**
     * Send a message to the Server
     *
     * @param messages Messages to send
     * @see net.ME1312.SubData.Client.Protocol.ForwardOnly Messages must <b><u>NOT</u></b> be tagged as Forward-Only
     */
    public abstract void sendMessage(MessageOut... messages);

    /**
     * Forward a message to another Client
     *
     * @param id Client ID
     * @param messages Messages to send
     * @see net.ME1312.SubData.Client.Protocol.Forwardable Messages must be tagged as Forwardable
     */
    public abstract void forwardMessage(UUID id, MessageOut... messages);

    /**
     * Get SubData's default Block Size
     *
     * @return Block Size
     */
    public abstract int getBlockSize();

    /**
     * Set SubData's Block Size for the current message
     *
     * @param size Block Size (null for default)
     */
    public abstract void tempBlockSize(Integer size);

    /**
     * Get the Client that connects the Server to us
     *
     * @deprecated The Client connection to the Server is this
     * @return This Client
     */
    @Deprecated
    public DataClient getConnection() {
        return this;
    }

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
     * Open an Async Data SubChannel
     *
     * @return New SubData Channel
     */
    public abstract DataClient newChannel() throws IOException;

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
