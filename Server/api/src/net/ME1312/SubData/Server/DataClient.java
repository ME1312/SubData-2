package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.SubData.Server.Library.DisconnectReason;
import net.ME1312.SubData.Server.Library.PingResponse;
import net.ME1312.SubData.Server.Protocol.MessageOut;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * SubData Client API Class
 */
public abstract class DataClient {
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
     * Ping the Client
     *
     * @param response Ping Response
     */
    public abstract void ping(Consumer<PingResponse> response);

    /**
     * Send a message to the Client
     *
     * @param messages Messages to send
     */
    public abstract void sendMessage(MessageOut... messages);

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
     * Gets the response from the Authorization Service
     *
     * @return Authorization Service Response
     */
    public abstract Object getAuthResponse();

    /**
     * Gets the Linked Handler
     *
     * @return Handler
     */
    public abstract ClientHandler getHandler();

    /**
     * Open an Async Data SubChannel
     *
     * @return New SubData Channel
     */
    public abstract void newChannel(Consumer<DataClient> client);

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
