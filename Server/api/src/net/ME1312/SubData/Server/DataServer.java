package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Callback.ReturnCallback;
import net.ME1312.Galaxi.Library.Util;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SubData Server API Class
 */
public abstract class DataServer {
    public final Events on = new Events();
    HashMap<String, Boolean> whitelist = new HashMap<String, Boolean>();

    /**
     * SubData Server Event API Class
     */
    public static class Events {
        LinkedList<ReturnCallback<DataClient, Boolean>> connect = new LinkedList<ReturnCallback<DataClient, Boolean>>();
        LinkedList<ReturnCallback<DataServer, Boolean>> close = new LinkedList<ReturnCallback<DataServer, Boolean>>();
        LinkedList<Callback<DataServer>> closed = new LinkedList<Callback<DataServer>>();
        private Events() {}

        /**
         * On Client Connect Event
         *
         * @param callbacks Callback
         */
        @SafeVarargs
        public final void connect(ReturnCallback<DataClient, Boolean>... callbacks) {
            connect.addAll(Arrays.asList(callbacks));
        }

        /**
         * On Listener Close Event
         *
         * @param callbacks Callback
         */
        @SafeVarargs
        public final void close(ReturnCallback<DataServer, Boolean>... callbacks) {
            close.addAll(Arrays.asList(callbacks));
        }

        /**
         * On Listener Closed Event
         *
         * @param callbacks Callback
         */
        @SafeVarargs
        public final void closed(Callback<DataServer>... callbacks) {
            closed.addAll(Arrays.asList(callbacks));
        }
    }

    /**
     * Get the Protocol for this Server
     *
     * @return Server Protocol
     */
    public abstract DataProtocol getProtocol();

    /**
     * Grabs a Client from the Network
     *
     * @param id Client ID
     * @return Client
     */
    public abstract DataClient getClient(UUID id);

    /**
     * Grabs all the Clients on the Network
     *
     * @return Client Map
     */
    public abstract Map<UUID, ? extends DataClient> getClients();

    /**
     * Remove a Client from the Network
     *
     * @param client Client to Kick
     * @throws IOException
     */
    public abstract void removeClient(DataClient client) throws IOException;

    /**
     * Remove a Client from the Network
     *
     * @param id Client ID
     * @throws IOException
     */
    public abstract void removeClient(UUID id) throws IOException;

    /**
     * Allow Access from an Address (Per-Server)
     *
     * @param address Address to allow
     */
    public void whitelist(String address) {
        if (Util.isNull(address)) throw new NullPointerException();
        whitelist.put(address, true);
    }

    boolean checkConnection(InetAddress address) {
        List<String> whitelist = new ArrayList<String>();
        whitelist.addAll(getProtocol().whitelist);
        for (String next : this.whitelist.keySet()) if (this.whitelist.get(next)) {
            whitelist.add(next);
        } else whitelist.remove(next);
        boolean whitelisted = false;
        Matcher regaddress = Pattern.compile("^(\\d{1,3}).(\\d{1,3}).(\\d{1,3}).(\\d{1,3})$").matcher(address.getHostAddress());
        if (regaddress.find()) {
            for (String allowed : whitelist) if (!whitelisted) {
                Matcher regallowed = Pattern.compile("^(\\d{1,3}|%).(\\d{1,3}|%).(\\d{1,3}|%).(\\d{1,3}|%)$").matcher(allowed);
                if (regallowed.find() && (
                        (regaddress.group(1).equals(regallowed.group(1)) || regallowed.group(1).equals("%")) &&
                                (regaddress.group(2).equals(regallowed.group(2)) || regallowed.group(2).equals("%")) &&
                                (regaddress.group(3).equals(regallowed.group(3)) || regallowed.group(3).equals("%")) &&
                                (regaddress.group(4).equals(regallowed.group(4)) || regallowed.group(4).equals("%"))
                )) whitelisted = true;
            }
        }

        return whitelisted;
    }

    /**
     * Revoke Access from an Address (Per-Server)
     *
     * @param address Address to deny
     */
    public void unwhitelist(String address) {
        if (Util.isNull(address)) throw new NullPointerException();
        whitelist.put(address, false);
    }

    /**
     * Drops all connections and close the SubData Listener
     *
     * @throws IOException
     */
    public abstract void close() throws IOException;

    /**
     * Wait for the listener to close
     *
     * @throws InterruptedException
     */
    public void waitFor() throws InterruptedException {
        while (!isClosed()) Thread.sleep(125);
    }

    /**
     * Get if the listener has been closed
     *
     * @return Closed Status
     */
    public abstract boolean isClosed();
}
