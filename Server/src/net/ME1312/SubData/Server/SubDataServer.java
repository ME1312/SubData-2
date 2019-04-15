package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Callback.ReturnCallback;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Encryption.NEH;
import net.ME1312.SubData.Server.Library.DebugUtil;
import net.ME1312.SubData.Server.Library.DisconnectReason;
import net.ME1312.SubData.Server.Library.Exception.EncryptionException;
import net.ME1312.SubData.Server.Protocol.Initial.InitPacketDeclaration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.*;

/**
 * SubData Server Class
 */
public class SubDataServer extends DataServer {
    private HashMap<UUID, SubDataClient> clients = new HashMap<UUID, SubDataClient>();
    private ServerSocket server;
    private String address;
    SubDataProtocol protocol;
    String cipher;

    SubDataServer(SubDataProtocol protocol, InetAddress address, int port, String cipher) throws IOException {
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

        this.cipher = cipher = (cipher != null)?cipher:"NULL"; // Validate Cipher
        String[] ciphers = (cipher.contains("/"))?cipher.split("/"):new String[]{cipher};
        Cipher last = NEH.get();
        for (String next : ciphers) {
            if (ciphers[0].equals(next)) {
                last = protocol.ciphers.get(next.toUpperCase());
            } else if (last instanceof CipherFactory) {
                final Cipher lastF = last;
                last = Util.getDespiteException(() -> ((CipherFactory) lastF).newCipher(next.toUpperCase()).name(), null);
            } else {
                last = null;
            }

            if (last == null)
                throw new EncryptionException("Unknown encryption type \"" + next + "\" in \"" + this.cipher + '\"');
        }

        protocol.log.info("Listening on " + this.address);
        new Thread(() -> {
            while (!server.isClosed()) {
                try {
                    addClient(server.accept());
                } catch (IOException e) {
                    if (!(e instanceof SocketException)) {
                        DebugUtil.logException(e, protocol.log);
                    }
                }
            }
        }, "SubDataServer::Connection_Listener(" + server.getLocalSocketAddress().toString() + ')').start();
    }

    /**
     * Get the underlying Server Socket
     *
     * @return Server Socket
     */
    public ServerSocket getSocket() {
        return server;
    }

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
        if (isWhitelisted(socket.getInetAddress())) {
            UUID id = Util.getNew(clients.keySet(), UUID::randomUUID);
            SubDataClient client = new SubDataClient(this, id, socket);

            boolean result = true;
            LinkedList<ReturnCallback<DataClient, Boolean>> events = on.connect;
            on.connect = new LinkedList<>();
            for (ReturnCallback<DataClient, Boolean> next : events) try {
                if (next != null) result = next.run(client) != Boolean.FALSE && result;
            } catch (Throwable e) {
                DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), protocol.log);
            }

            if (result) {
                clients.put(id, client);
                protocol.log.info(client.getAddress().toString() + " has connected");

                client.sendPacket(new InitPacketDeclaration());
                client.read();
                return client;
            } else {
                client.close(DisconnectReason.CLOSE_REQUESTED);
                protocol.log.info(socket.getInetAddress().toString() + " attempted to connect, but was blocked");
                return null;
            }
        } else {
            protocol.log.info(socket.getInetAddress().toString() + " attempted to connect, but isn't white-listed");
            socket.close();
            return null;
        }
    }

    public SubDataClient getClient(UUID id) {
        if (Util.isNull(id)) throw new NullPointerException();
        return clients.get(id);
    }

    public Map<UUID, ? extends SubDataClient> getClients() {
        return new HashMap<>(clients);
    }

    public void removeClient(DataClient client) throws IOException {
        if (Util.isNull(client)) throw new NullPointerException();
        removeClient(client.getID());
    }

    public void removeClient(UUID id) throws IOException {
        if (Util.isNull(id)) throw new NullPointerException();
        if (clients.keySet().contains(id)) {
            SubDataClient client = clients.get(id);
            clients.remove(id);
            client.close();
            protocol.log.info(client.getAddress().toString() + " has disconnected");
        }
    }

    public void close() throws IOException {
        boolean result = true;
        LinkedList<ReturnCallback<DataServer, Boolean>> events = on.close;
        on.close = new LinkedList<>();
        for (ReturnCallback<DataServer, Boolean> next : events) try {
            if (next != null) result = next.run(this) != Boolean.FALSE && result;
        } catch (Throwable e) {
            DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), protocol.log);
        }

        if (result) {
            while (clients.size() > 0) {
                SubDataClient client = (SubDataClient) clients.values().toArray()[0];
                client.close();
                Util.isException(client::waitFor);
            }
            server.close();

            LinkedList<Callback<DataServer>> events2 = on.closed;
            for (Callback<DataServer> next : events2) try {
                if (next != null) next.run(this);
            } catch (Throwable e) {
                DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), protocol.log);
            }

            protocol.log.info("Listener " + this.address + " has been closed");
        }
    }

    public boolean isClosed() {
        return server.isClosed();
    }


}
