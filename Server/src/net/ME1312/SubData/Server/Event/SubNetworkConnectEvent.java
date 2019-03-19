package net.ME1312.SubData.Server.Event;

import net.ME1312.Galaxi.Library.Event.CancellableEvent;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.DataServer;

import java.net.InetAddress;

/**
 * SubData Network Connect Event
 */
public class SubNetworkConnectEvent extends CancellableEvent {
    private DataServer network;
    private InetAddress address;

    /**
     * SubData Network Connect Event
     */
    public SubNetworkConnectEvent(DataServer network, InetAddress address) {
        if (Util.isNull(network, address)) throw new NullPointerException();
        this.network = network;
        this.address = address;
    }

    /**
     * Get the network the client is trying to connect to
     *
     * @return SubData Network
     */
    public DataServer getNetwork() {
        return network;
    }

    /**
     * Get the address of the connecting client
     *
     * @return Client address
     */
    public InetAddress getAddress() {
        return address;
    }
}