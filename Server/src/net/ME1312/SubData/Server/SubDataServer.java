package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Callback.ReturnCallback;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Encryption.NEH;
import net.ME1312.SubData.Server.Library.DebugUtil;
import net.ME1312.SubData.Server.Library.DisconnectReason;
import net.ME1312.SubData.Server.Library.Exception.EncryptionException;
import net.ME1312.SubData.Server.Protocol.Initial.InitPacketDeclaration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * SubData Server Class
 */
public class SubDataServer extends DataServer {
    private HashMap<UUID, SubDataClient> clients = new HashMap<UUID, SubDataClient>();
    private ServerSocket server;
    private String address;
    SubDataProtocol protocol;
    Value<Long> timeout;
    Callback<Runnable> scheduler;
    String cipher;
    Logger log;

    SubDataServer(SubDataProtocol protocol, Callback<Runnable> scheduler, Logger log, InetAddress address, int port, String cipher) throws IOException {
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
        this.timeout = protocol.timeout;
        this.scheduler = scheduler;
        this.log = log;

        this.cipher = cipher = (cipher != null)?cipher:"NULL"; // Validate Cipher
        String[] ciphers = (cipher.contains("/"))?cipher.split("/"):new String[]{cipher};
        Cipher last = NEH.get();
        for (String next : ciphers) {
            if (ciphers[0].equals(next)) {
                last = protocol.ciphers.get(next.toUpperCase());
            } else if (last instanceof CipherFactory) {
                final Cipher lastF = last;
                last = Util.getDespiteException(() -> ((CipherFactory) lastF).newCipher(next.toUpperCase()).key(), null);
            } else {
                last = null;
            }

            if (last == null)
                throw new EncryptionException("Unknown encryption type \"" + next + "\" in \"" + this.cipher + '\"');
        }

        log.info("Listening on " + this.address);
        new Thread(() -> {
            while (!server.isClosed()) {
                try {
                    addClient(server.accept());
                } catch (IOException e) {
                    if (!(e instanceof SocketException)) {
                        DebugUtil.logException(e, log);
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
     * Get SubData's Initialization Timer
     *
     * @return Timeout Time
     */
    public long getTimeout() {
        return timeout.value();
    }

    /**
     * Get SubData's Initialization Timer
     *
     * @param size Timeout Time (null for super)
     */
    public void setTimeout(Long size) {
        if (size == null) {
            timeout = protocol.timeout;
        } else timeout = new Container<>(size);
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
            SubDataClient client = addClient(new SubDataClient(this, socket));
            if (client != null) client.read();
            return client;
        } else {
            log.info(socket.getInetAddress().toString() + " attempted to connect, but isn't white-listed");
            socket.close();
            return null;
        }
    }

    /**
     * Add a Client to the Network
     *
     * @param client Client to add
     * @throws IOException
     */
    private SubDataClient addClient(SubDataClient client) throws IOException {
        boolean result = true;
        Util.isException(() -> Util.reflect(DataClient.class.getDeclaredField("id"), client, Util.getNew(clients.keySet(), UUID::randomUUID)));
        LinkedList<ReturnCallback<DataClient, Boolean>> events = new LinkedList<>(on.connect);
        for (ReturnCallback<DataClient, Boolean> next : events) try {
            if (next != null) result = next.run(client) != Boolean.FALSE && result;
        } catch (Throwable e) {
            DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), log);
        }

        if (result) {
            clients.put(client.getID(), client);
            log.info(client.getAddress().toString() + " has connected");

            client.sendPacket(new InitPacketDeclaration());
            return client;
        } else {
            client.close(DisconnectReason.CLOSE_REQUESTED);
            log.info(client.getAddress().toString() + " attempted to connect, but was blocked");
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

    public void removeClient(DataClient client) {
        if (Util.isNull(client)) throw new NullPointerException();
        removeClient(client.getID());
    }

    public void removeClient(UUID id) {
        if (Util.isNull(id)) throw new NullPointerException();
        if (clients.keySet().contains(id)) {
            SubDataClient client = clients.get(id);
            clients.remove(id);
            client.close();
        }
    }

    public void close() throws IOException {
        boolean result = true;
        LinkedList<ReturnCallback<DataServer, Boolean>> events = new LinkedList<>(on.close);
        for (ReturnCallback<DataServer, Boolean> next : events) try {
            if (next != null) result = next.run(this) != Boolean.FALSE && result;
        } catch (Throwable e) {
            DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), log);
        }

        if (result) {
            while (clients.size() > 0) {
                SubDataClient client = (SubDataClient) clients.values().toArray()[0];
                client.close();
                Util.isException(client::waitFor);
            }
            server.close();

            scheduler.run(() -> {
                LinkedList<Callback<DataServer>> events2 = new LinkedList<>(on.closed);
                for (Callback<DataServer> next : events2) try {
                    if (next != null) next.run(this);
                } catch (Throwable e) {
                    DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), log);
                }
            });

            log.info("Listener " + this.address + " has been closed");
        }
    }

    public boolean isClosed() {
        return server.isClosed();
    }


}
