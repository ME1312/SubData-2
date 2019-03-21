package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Encryption.NEH;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.*;
import java.util.*;

/**
 * SubData Server Class
 */
public class SubDataServer extends DataServer {
    private HashMap<String, SubDataClient> clients = new HashMap<String, SubDataClient>();
    private Runnable shutdown;
    private ServerSocket server;
    private String address;
    SubDataProtocol protocol;
    String cipher;

    SubDataServer(SubDataProtocol protocol, InetAddress address, int port, String cipher, Runnable shutdown) throws IOException {
        if (Util.isNull(protocol)) throw new NullPointerException();
        if (address == null) {
            this.server = new ServerSocket(port, protocol.MAX_QUEUE);
            this.address = "/0.0.0.0:" + port;
            whitelist("127.0.0.1");
        } else {
            this.server = new ServerSocket(port, protocol.MAX_QUEUE, address);
            this.address = server.getLocalSocketAddress().toString();
            whitelist(address.getHostAddress());
        }
        this.protocol = protocol;
        this.shutdown = shutdown;

        this.cipher = cipher = (cipher != null)?cipher:"NULL"; // Validate Cipher
        String[] ciphers = (cipher.contains("/"))?cipher.split("/"):new String[]{cipher};
        Cipher ccurrent = NEH.get();
        for (String cnext : ciphers) {
            if (ciphers[0].equals(cnext)) {
                ccurrent = protocol.ciphers.get(cnext.toUpperCase());
            } else if (ccurrent instanceof CipherFactory) {
                final Cipher ccurrent2 = ccurrent;
                ccurrent = Util.getDespiteException(() -> ((CipherFactory) ccurrent2).newCipher(cnext.toUpperCase()).name(), null);
            } else {
                ccurrent = null;
            }

            if (ccurrent == null)
                throw new IllegalArgumentException("Unknown encryption type \"" + cnext + "\" in \"" + this.cipher + '\"');
        }

        protocol.log.info("Listening on " + this.address);
        new Thread(() -> {
            while (!server.isClosed()) {
                try {
                    addClient(server.accept());
                } catch (IOException e) {
                    if (!(e instanceof SocketException)) {
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        protocol.log.severe(sw.toString());
                    }
                }
            }
        }, "SubDataServer::Connection_Listener").start();
    }

    /**
     * Get the underlying Server Socket
     *
     * @return Server Socket
     */
    public ServerSocket getSocket() {
        return server;
    }

    /**
     * Get the Protocol for this Server
     *
     * @return Server Protocol
     */
    public SubDataProtocol getProtocol() {
        return protocol;
    }

    /**
     * Add a Client to the Network
     *
     * @param socket Client to add
     * @throws IOException
     */
    private SubDataClient addClient(Socket socket) throws IOException {
        if (Util.isNull(socket)) throw new NullPointerException();
        if (checkConnection(socket.getInetAddress())) {
            SubDataClient client = new SubDataClient(this, socket);
            protocol.log.info(client.getAddress().toString() + " has connected");
            clients.put(client.getAddress().toString(), client);
            return client;
        } else {
            protocol.log.info(socket.getInetAddress().toString() + " attempted to connect, but isn't white-listed");
            socket.close();
            return null;
        }
    }

    /**
     * Grabs a Client from the Network
     *
     * @param socket Socket to search
     * @return Client
     */
    public SubDataClient getClient(Socket socket) {
        if (Util.isNull(socket)) throw new NullPointerException();
        return getClient(new InetSocketAddress(socket.getInetAddress(), socket.getPort()));
    }

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public SubDataClient getClient(InetSocketAddress address) {
        if (Util.isNull(address)) throw new NullPointerException();
        return getClient(address.toString());
    }

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public SubDataClient getClient(String address) {
        if (Util.isNull(address)) throw new NullPointerException();
        return clients.get(address);
    }

    /**
     * Grabs all the Clients on the Network
     *
     * @return Client List
     */
    public Collection<DataClient> getClients() {
        return new ArrayList<DataClient>(clients.values());
    }

    /**
     * Remove a Client from the Network
     *
     * @param client Client to Kick
     * @throws IOException
     */
    public void removeClient(SubDataClient client) throws IOException {
        if (Util.isNull(client)) throw new NullPointerException();
        removeClient(client.getAddress());
    }

    /**
     * Remove a Client from the Network
     *
     * @param address Address to Kick
     * @throws IOException
     */
    public void removeClient(InetSocketAddress address) throws IOException {
        if (Util.isNull(address)) throw new NullPointerException();
        removeClient(address.toString());
    }

    /**
     * Remove a Client from the Network
     *
     * @param address Address to Kick
     * @throws IOException
     */
    public void removeClient(String address) throws IOException {
        if (Util.isNull(address)) throw new NullPointerException();
        if (clients.keySet().contains(address)) {
            SubDataClient client = clients.get(address);
            clients.remove(address);
            client.close();
            protocol.log.info(client.getAddress().toString() + " has disconnected");
        }
    }

    /**
     * Drops all connections and closes the SubData Listener
     *
     * @throws IOException
     */
    public void close() throws IOException {
        while(clients.size() > 0) {
            removeClient((SubDataClient) clients.values().toArray()[0]);
        }
        server.close();
        protocol.log.info("Listener " + this.address + " has been closed");
        if (shutdown != null) shutdown.run();
    }
}
