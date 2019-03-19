package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Galaxi;
import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Encryption.NEH;
import net.ME1312.SubData.Server.Event.SubNetworkDisconnectEvent;
import net.ME1312.SubData.Server.Protocol.Internal.PacketRecieveMessage;
import net.ME1312.SubData.Server.Protocol.Internal.PacketSendMessage;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketOut;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class SubDataServer extends DataServer {
    static final HashMap<Class<? extends PacketOut>, Integer> pOut = new HashMap<Class<? extends PacketOut>, Integer>();
    static final HashMap<Integer, PacketIn> pIn = new HashMap<Integer, PacketIn>();
    static Logger log = new Logger("SubData");
    private static int MAX_QUEUE = 64;
    private static final CipherFactory staticFactory = new CipherFactory() {
        private HashMap<String, Util.ReturnRunnable<NamedContainer<Cipher, String>>> ciphers = new HashMap<String, Util.ReturnRunnable<NamedContainer<Cipher, String>>>();
        private NamedContainer<Cipher, String> nullcipher = new NamedContainer<>(NEH.get(), null);

        @Override
        public NamedContainer<Cipher, String> getCipher(String handle) {
            return ciphers.getOrDefault(handle.toUpperCase(), () -> nullcipher).run();
        }

        @Override
        public void addCipher(String handle, Util.ReturnRunnable<NamedContainer<Cipher, String>> generator) {
            if (Util.isNull(generator)) throw new NullPointerException();
            handle = handle.toUpperCase();
            if (!ciphers.keySet().contains(handle)) ciphers.put(handle, generator);
        }

        @Override
        public void removeCipher(String handle) {
            ciphers.remove(handle.toUpperCase());
        }
    };
    private HashMap<String, Client> clients = new HashMap<String, Client>();
    private ServerSocket server;
    private String cipher;
    private Runnable shutdown;

    static {
        staticFactory.addCipher("NULL", () -> new NamedContainer<>(NEH.get(), null));
        staticFactory.addCipher("NONE", () -> new NamedContainer<>(NEH.get(), null));

        registerPacket(0x0000, PacketSendMessage.class);
        registerPacket(0x0000, new PacketRecieveMessage());
    }

    /**
     * SubData Server Instance
     *
     * @param address Bind Address (or null for all)
     * @param port Port Number
     * @param cipher Cipher (or null for none)
     * @throws IOException
     */
    public SubDataServer(InetAddress address, int port, String cipher) throws IOException {
        this(address, port, cipher, null);
    }

    /**
     * SubData Server Instance
     *
     * @param address Bind Address (or null for all)
     * @param port Port Number
     * @param cipher Cipher (or null for none)
     * @param shutdown Shutdown Event
     * @throws IOException
     */
    public SubDataServer(InetAddress address, int port, String cipher, Runnable shutdown) throws IOException {
        if (address == null) {
            server = new ServerSocket(port, MAX_QUEUE);
            allowConnection("127.0.0.1");
        } else {
            server = new ServerSocket(port, MAX_QUEUE, address);
            allowConnection(address.getHostAddress());
        }
        this.shutdown = shutdown;
        this.cipher = (cipher != null)?cipher:"NULL";

        new Thread(() -> {
            while (!server.isClosed()) {
                try {
                    addClient(server.accept());
                } catch (IOException e) {
                    if (!(e instanceof SocketException)) log.error.println(e);
                }
            }
        }, "SubDataServer::Connection_Listener").start();
    }

    public ServerSocket getServer() {
        return server;
    }

    /**
     * Add a Client to the Network
     *
     * @param socket Client to add
     * @throws IOException
     */
    private Client addClient(Socket socket) throws IOException {
        if (Util.isNull(socket)) throw new NullPointerException();
        if (checkConnection(socket.getInetAddress())) {
            Client client = new Client(this, socket);
            log.info.println(client.getAddress().toString() + " has connected");
            clients.put(client.getAddress().toString(), client);
            return client;
        } else {
            log.info.println(socket.getInetAddress().toString() + " attempted to connect, but isn't white-listed");
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
    public Client getClient(Socket socket) {
        if (Util.isNull(socket)) throw new NullPointerException();
        return getClient(new InetSocketAddress(socket.getInetAddress(), socket.getPort()));
    }

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public Client getClient(InetSocketAddress address) {
        if (Util.isNull(address)) throw new NullPointerException();
        return getClient(address.toString());
    }

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public Client getClient(String address) {
        if (Util.isNull(address)) throw new NullPointerException();
        return clients.get(address);
    }

    /**
     * Grabs all the Clients on the Network
     *
     * @return Client List
     */
    public Collection<Client> getClients() {
        return clients.values();
    }

    /**
     * Remove a Client from the Network
     *
     * @param client Client to Kick
     * @throws IOException
     */
    public void removeClient(Client client) throws IOException {
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
            Client client = clients.get(address);
            Galaxi.getInstance().getPluginManager().executeEvent(new SubNetworkDisconnectEvent(this, client));
            clients.remove(address);
            client.disconnect();
            log.info.println(client.getAddress().toString() + " has disconnected");
        }
    }

    /**
     * Register PacketIn to the Network
     *
     * @param id Packet ID (as an unsigned 16-bit value)
     * @param packet PacketIn to register
     */
    public static void registerPacket(int id, PacketIn packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (id > 65535 || id < 0) throw new IllegalArgumentException("Packet ID is not in range (0-65535): " + id);
        pIn.put(id, packet);
    }

    /**
     * Unregister PacketIn from the Network
     *
     * @param packet PacketIn to unregister
     */
    public static void unregisterPacket(PacketIn packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        List<Integer> search = new ArrayList<Integer>();
        search.addAll(pIn.keySet());
        for (int id : search) if (pIn.get(id).equals(packet)) {
            pIn.remove(id);
        }
    }

    /**
     * Register PacketOut to the Network
     *
     * @param id Packet ID (as an unsigned 16-bit value)
     * @param packet PacketOut to register
     */
    public static void registerPacket(int id, Class<? extends PacketOut> packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (id > 65535 || id < 0) throw new IllegalArgumentException("Packet ID is not in range (0-65535): " + id);
        pOut.put(packet, id);
    }

    /**
     * Unregister PacketOut to the Network
     *
     * @param packet PacketOut to unregister
     */
    public static void unregisterPacket(Class<? extends PacketOut> packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (pOut.keySet().contains(packet)) pOut.remove(packet);
    }

    /**
     * Grab PacketIn Instance via ID
     *
     * @param id Packet ID (as an unsigned 16-bit value)
     * @return PacketIn
     */
    public static PacketIn getPacket(int id) {
        return pIn.get(id);
    }

    /**
     * Drops All Connections and Stops the SubData Listener
     *
     * @throws IOException
     */
    public void destroy() throws IOException {
        while(clients.size() > 0) {
            removeClient((Client) clients.values().toArray()[0]);
        }
        server.close();
        if (shutdown != null) shutdown.run();
    }
}
