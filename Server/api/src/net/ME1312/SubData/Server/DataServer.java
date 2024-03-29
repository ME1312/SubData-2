package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Util;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SubData Server API Class
 */
public abstract class DataServer {
    private final static Pattern REG_ADRESS_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private final static Pattern REG_ALLOWED_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:/(\\d{1,2}))?$");

    public final Events on = new Events();
    HashMap<String, Boolean> whitelist = new HashMap<String, Boolean>();

    /**
     * SubData Server Event API Class
     */
    public static class Events {
        LinkedList<Function<DataClient, Boolean>> connect = new LinkedList<Function<DataClient, Boolean>>();
        LinkedList<Function<DataServer, Boolean>> close = new LinkedList<Function<DataServer, Boolean>>();
        LinkedList<Consumer<DataServer>> closed = new LinkedList<Consumer<DataServer>>();
        private Events() {}

        /**
         * On Client Connect Event
         *
         * @param callbacks Callback
         */
        @SafeVarargs
        public final void connect(Function<DataClient, Boolean>... callbacks) {
            connect.addAll(Arrays.asList(callbacks));
        }

        /**
         * On Listener Close Event
         *
         * @param callbacks Callback
         */
        @SafeVarargs
        public final void close(Function<DataServer, Boolean>... callbacks) {
            close.addAll(Arrays.asList(callbacks));
        }

        /**
         * On Listener Closed Event
         *
         * @param callbacks Callback
         */
        @SafeVarargs
        public final void closed(Consumer<DataServer>... callbacks) {
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
        Util.nullpo(address);
        whitelist.put(address, true);
    }

    boolean isWhitelisted(InetAddress address) {
        List<String> whitelist = new ArrayList<>(getProtocol().whitelist);
        for (String next : this.whitelist.keySet()) if (this.whitelist.get(next)) {
            whitelist.add(next);
        } else whitelist.remove(next);

        boolean whitelisted = false;
        Matcher regMatcher = REG_ADRESS_PATTERN.matcher(address.getHostAddress());
        if (regMatcher.find()) {
            int rip = 0;
            for (int i = 1; i <= 4; i++) {
                int octet = Integer.parseInt(regMatcher.group(i));
                if (octet > 255) octet = 255;

                rip = (rip << 8) + octet;
            }

            for (String allowed : whitelist) if (!whitelisted) {
                Matcher regallowed = REG_ALLOWED_PATTERN.matcher(allowed);
                if (regallowed.find()) {
                    int sub = (regallowed.group(5) == null)?32:Integer.parseInt(regallowed.group(5));
                    if (sub > 32) sub = 32;
                    if (sub >  0) sub = 0xffffffff << (32 - sub);
                    else sub = 0;

                    int aip = 0;
                    for (int i = 1; i <= 4; i++) {
                        int octet = Integer.parseInt(regallowed.group(i));
                        if (octet > 255) octet = 255;

                        aip = (aip << 8) + octet;
                    }

                    if ((rip & sub) == aip) whitelisted = true;
                }
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
        Util.nullpo(address);

        if (getProtocol().whitelist.contains(address)) {
            whitelist.put(address, false);
        } else whitelist.remove(address);
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
