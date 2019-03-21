package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Protocol.MessageOut;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SubData Server API Class
 */
public abstract class DataServer {
    HashMap<String, Boolean> whitelist = new HashMap<String, Boolean>();

    /**
     * Get the Protocol for this Server
     *
     * @return Server Protocol
     */
    public abstract DataProtocol getProtocol();

    /**
     * Grabs a Client from the Network
     *
     * @param socket Socket to search
     * @return Client
     */
    public abstract DataClient getClient(Socket socket);

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public abstract DataClient getClient(InetSocketAddress address);

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public abstract DataClient getClient(String address);

    /**
     * Grabs all the Clients on the Network
     *
     * @return Client List
     */
    public abstract Collection<DataClient> getClients();

    /**
     * Broadcast a Message to everything on the Network<br>
     * <b>Warning:</b> There are usually different types of applications on the network at once, they may not recognise the same message handles
     *
     * @param message Message to send
     */
    public void broadcastMessage(MessageOut message) {
        if (Util.isNull(message)) throw new NullPointerException();
        List<DataClient> clients = new ArrayList<DataClient>();
        clients.addAll(getClients());
        for (DataClient client : clients) {
            client.sendMessage(message);
        }
    }

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
}
