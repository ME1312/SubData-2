package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Callback.ReturnCallback;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.DisconnectReason;
import net.ME1312.SubData.Server.Library.PingResponse;
import net.ME1312.SubData.Server.Protocol.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;

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
        LinkedList<Callback<DataClient>> ready = new LinkedList<Callback<DataClient>>();
        LinkedList<ReturnCallback<DataClient, Boolean>> close = new LinkedList<ReturnCallback<DataClient, Boolean>>();
        LinkedList<Callback<Pair<DisconnectReason, DataClient>>> closed = new LinkedList<Callback<Pair<DisconnectReason, DataClient>>>();
        private Events() {}

        /**
         * On Connection Ready Event
         *
         * @param callbacks Callback
         */
        @SafeVarargs
        public final void ready(Callback<DataClient>... callbacks) {
            ready.addAll(Arrays.asList(callbacks));
        }

        /**
         * On Connection Close Event
         *
         * @param callbacks Callback
         */
        @SafeVarargs
        public final void close(ReturnCallback<DataClient, Boolean>... callbacks) {
            close.addAll(Arrays.asList(callbacks));
        }

        /**
         * On Connection Closed Event
         *
         * @param callbacks Callback
         */
        @SafeVarargs
        public final void closed(Callback<Pair<DisconnectReason, DataClient>>... callbacks) {
            closed.addAll(Arrays.asList(callbacks));
        }
    }

    /**
     * Ping the Client
     *
     * @param response Ping Response
     */
    public abstract void ping(Callback<PingResponse> response);

    /**
     * Send a message to the Client
     *
     * @param messages Messages to send
     */
    public abstract void sendMessage(MessageOut... messages);

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
    public abstract void newChannel(Callback<DataClient> client);

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
