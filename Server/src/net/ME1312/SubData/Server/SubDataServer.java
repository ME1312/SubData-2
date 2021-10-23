package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Value;
import net.ME1312.Galaxi.Library.Try;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * SubData Server Class
 */
public class SubDataServer extends DataServer {
    private final HashMap<UUID, SubDataClient> clients = new HashMap<UUID, SubDataClient>();
    private final ServerSocket server;
    private final String address;
    SubDataProtocol protocol;
    Value<Long> timeout;
    Consumer<Runnable> scheduler;
    String cipher;
    Logger log;

    SubDataServer(SubDataProtocol protocol, Consumer<Runnable> scheduler, Logger log, InetAddress address, int port, String cipher) throws IOException {
        Util.nullpo(protocol);
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
                last = Try.all.get(() -> ((CipherFactory) lastF).newCipher(next.toUpperCase()).key());
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
        Util.nullpo(socket);
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
     */
    private SubDataClient addClient(SubDataClient client) {
        boolean result = true;
        Try.all.run(() -> Util.reflect(DataClient.class.getDeclaredField("id"), client, Util.getNew(clients.keySet(), UUID::randomUUID)));
        LinkedList<Function<DataClient, Boolean>> events = new LinkedList<>(on.connect);
        for (Function<DataClient, Boolean> next : events) try {
            if (next != null) result = next.apply(client) != Boolean.FALSE && result;
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
        Util.nullpo(id);
        return clients.get(id);
    }

    public Map<UUID, ? extends SubDataClient> getClients() {
        return new HashMap<>(clients);
    }

    public void removeClient(DataClient client) {
        Util.nullpo(client);
        removeClient(client.getID());
    }

    public void removeClient(UUID id) {
        Util.nullpo(id);
        if (clients.containsKey(id)) {
            SubDataClient client = clients.get(id);
            clients.remove(id);
            client.close();
        }
    }

    public void close() throws IOException {
        boolean result = true;
        LinkedList<Function<DataServer, Boolean>> events = new LinkedList<>(on.close);
        for (Function<DataServer, Boolean> next : events) try {
            if (next != null) result = next.apply(this) != Boolean.FALSE && result;
        } catch (Throwable e) {
            DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), log);
        }

        if (result) {
            while (clients.size() > 0) {
                SubDataClient client = (SubDataClient) clients.values().toArray()[0];
                client.close();
                Try.all.run(client::waitFor);
            }
            server.close();

            scheduler.accept(() -> {
                LinkedList<Consumer<DataServer>> events2 = new LinkedList<>(on.closed);
                for (Consumer<DataServer> next : events2) try {
                    if (next != null) next.accept(this);
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
