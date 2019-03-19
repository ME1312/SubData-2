package net.ME1312.SubData.Server.Event;

import net.ME1312.Galaxi.Library.Event.CancellableEvent;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Client;
import net.ME1312.SubData.Server.DataServer;

/**
 * SubData Network Disconnect Event
 */
public class SubNetworkDisconnectEvent extends CancellableEvent {
    private DataServer network;
    private Client client;

    /**
     * SubData Network Disconnect Event
     */
    public SubNetworkDisconnectEvent(DataServer network, Client client) {
        if (Util.isNull(network, client)) throw new NullPointerException();
        this.network = network;
        this.client = client;
    }

    /**
     * Get the network the client is disconnecting from
     *
     * @return SubData Network
     */
    public DataServer getNetwork() {
        return network;
    }

    /**
     * Get the disconnecting client
     *
     * @return Client
     */
    public Client getClient() {
        return client;
    }

}