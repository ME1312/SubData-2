package net.ME1312.SubData.Client;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Callback.ReturnCallback;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.PrimitiveContainer;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Encryption.NEH;
import net.ME1312.SubData.Client.Library.*;
import net.ME1312.SubData.Client.Library.Exception.*;
import net.ME1312.SubData.Client.Library.PingResponse;
import net.ME1312.SubData.Client.Protocol.*;
import net.ME1312.SubData.Client.Protocol.Initial.InitPacketDeclaration;
import net.ME1312.SubData.Client.Protocol.Initial.InitialPacket;
import net.ME1312.SubData.Client.Protocol.Initial.InitialProtocol;
import net.ME1312.SubData.Client.Protocol.Internal.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.logging.Logger;

import static net.ME1312.SubData.Client.Library.ConnectionState.*;
import static net.ME1312.SubData.Client.Library.DisconnectReason.*;

/**
 * SubData Client Class
 */
public class SubDataClient extends DataClient implements SubDataSender {
    private Socket socket;
    private LinkedList<PacketOut> queue;
    private HashMap<ConnectionState, LinkedList<PacketOut>> statequeue;
    private BufferedInputStream in;
    private OutputStreamL1 out;
    private Container<Long> bs;
    private SubDataProtocol protocol;
    private Cipher cipher = NEH.get();
    private int cipherlevel = 0;
    private ObjectMap<?> login;
    private ConnectionState state;
    private DisconnectReason isdcr;
    private Callback<Runnable> scheduler;
    private Object[] constructor;
    private SubDataClient next;
    private Logger log;

    SubDataClient(SubDataProtocol protocol, Callback<Runnable> scheduler, Logger log, InetAddress address, int port, ObjectMap<?> login) throws IOException {
        if (Util.isNull(address, port)) throw new NullPointerException();
        this.protocol = protocol;
        this.bs = protocol.bs;
        this.login = login;
        this.scheduler = scheduler;
        this.log = log;
        this.state = PRE_INITIALIZATION;
        this.isdcr = PROTOCOL_MISMATCH;
        this.socket = new Socket(address, port);
        this.in = new BufferedInputStream(socket.getInputStream(), (bs.get() > Integer.MAX_VALUE) ? Integer.MAX_VALUE : bs.get().intValue());
        this.out = new OutputStreamL1(log, socket.getOutputStream(), bs.get());
        this.queue = null;
        this.statequeue = new HashMap<>();
        this.constructor = new Object[]{
                scheduler,
                log,
                address,
                port,
                login
        };

        log.info("Connected to " + socket.getRemoteSocketAddress());
        read();
    }

    private void read(SubDataSender sender, PrimitiveContainer<Boolean> reset, InputStream stream) {
        try {
            BufferedInputStream data = new BufferedInputStream(stream, (bs.get() > Integer.MAX_VALUE) ? Integer.MAX_VALUE : bs.get().intValue());
            ByteArrayOutputStream pending = new ByteArrayOutputStream();
            int id = -1, version = -1;

            int b, position = 0;
            while (position < 4 && (b = data.read()) != -1) {
                position++;
                pending.write(b);
                switch (position) {
                    case 2:
                        id = (int) UnsignedDataHandler.fromUnsigned(pending.toByteArray());
                        pending.reset();
                        break;
                    case 4:
                        version = (int) UnsignedDataHandler.fromUnsigned(pending.toByteArray());
                        pending.reset();
                        break;
                }
            }

            // Step 4 // Create a detached data forwarding InputStream
            if (state != CLOSED && id >= 0 && version >= 0) {
                PrimitiveContainer<Boolean> open = new PrimitiveContainer<>(true);
                InputStream forward = new InputStream() {
                    @Override
                    public int read() throws IOException {
                        if (open.value) {
                            int b = data.read();
                            if (b < 0) close();
                            return b;
                        } else return -1;
                    }

                    @Override
                    public void close() throws IOException {
                        open.value = false;
                        while (data.read() != -1);
                    }
                };
                if (state == PRE_INITIALIZATION && id != 0x0000) {
                    DebugUtil.logException(new IllegalStateException(getAddress().toString() + ": Only InitPacketDeclaration (0x0000) may be received during the PRE_INITIALIZATION stage: [" + DebugUtil.toHex(0xFFFF, id) + ", " + DebugUtil.toHex(0xFFFF, version) + "]"), log);
                    close(PROTOCOL_MISMATCH);
                } else if (state == CLOSING && id != 0xFFFE) {
                    forward.close(); // Suppress other packets during the CLOSING stage
                } else {
                    HashMap<Integer, PacketIn> pIn = (state.asInt() >= POST_INITIALIZATION.asInt())?protocol.pIn:Util.reflect(InitialProtocol.class.getDeclaredField("pIn"), null);
                    if (!pIn.keySet().contains(id)) throw new IllegalPacketException(getAddress().toString() + ": Could not find handler for packet: [" + DebugUtil.toHex(0xFFFF, id) + ", " + DebugUtil.toHex(0xFFFF, version) + "]");
                    PacketIn packet = pIn.get(id);
                    if (sender instanceof ForwardedDataSender && !(packet instanceof Forwardable)) throw new IllegalSenderException(getAddress().toString() + ": The handler does not support forwarded packets: [" + DebugUtil.toHex(0xFFFF, id) + ", " + DebugUtil.toHex(0xFFFF, version) + "]");
                    if (sender instanceof SubDataClient && packet instanceof ForwardOnly) throw new IllegalSenderException(getAddress().toString() + ": The handler does not support non-forwarded packets: [" + DebugUtil.toHex(0xFFFF, id) + ", " + DebugUtil.toHex(0xFFFF, version) + "]");
                    if (!packet.isCompatible(version)) throw new IllegalPacketException(getAddress().toString() + ": The handler does not support this packet version (" + DebugUtil.toHex(0xFFFF, packet.version()) + "): [" + DebugUtil.toHex(0xFFFF, id) + ", " + DebugUtil.toHex(0xFFFF, version) + "]");

                    // Step 5 // Invoke the Packet
                    if (state == PRE_INITIALIZATION && !(packet instanceof InitPacketDeclaration)) {
                        DebugUtil.logException(new IllegalStateException(getAddress().toString() + ": Only " + InitPacketDeclaration.class.getCanonicalName() + " may be received during the PRE_INITIALIZATION stage: " + packet.getClass().getCanonicalName()), log);
                        close(PROTOCOL_MISMATCH);
                    } else if (state == CLOSING && !(packet instanceof PacketDisconnectUnderstood)) {
                        forward.close(); // Suppress other packets during the CLOSING stage
                    } else {
                        scheduler.run(() -> {
                            try {
                                packet.receive(sender);

                                if (packet instanceof PacketStreamIn) {
                                    ((PacketStreamIn) packet).receive(sender, forward);
                                } else forward.close();
                            } catch (Throwable e) {
                                DebugUtil.logException(new InvocationTargetException(e, getAddress().toString() + ": Exception while running packet handler"), log);
                                Util.isException(forward::close);

                                if (state.asInt() <= INITIALIZATION.asInt())
                                    Util.isException(() -> close(PROTOCOL_MISMATCH)); // Issues during the init stages are signs of a PROTOCOL_MISMATCH
                            }
                        });
                        if (sender == this) while (open.value) Thread.sleep(125);
                    }
                }
            }
        } catch (Exception e) {
            if (!reset.value) try {
                if (!(e instanceof SocketException && !Boolean.getBoolean("subdata.debug"))) {
                    DebugUtil.logException(e, log);
                } if (!(e instanceof SocketException)) {
                    close(UNHANDLED_EXCEPTION);
                } else close(CONNECTION_INTERRUPTED);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
    void read() {
        if (!socket.isClosed()) new Thread(() -> {
            PrimitiveContainer<Boolean> reset = new PrimitiveContainer<>(false);
            try {
                // Step 1 // Parse Escapes in the Encrypted Data
                InputStream raw = new InputStreamL1(in, () -> {
                    if (state != PRE_INITIALIZATION) reset.value = true;
                }, () -> {
                    if (!socket.isClosed()) {
                        SubDataClient.this.read();
                    } else Util.isException(() -> close(CONNECTION_INTERRUPTED));
                });

                PipedInputStream data = new PipedInputStream(1024);
                PipedOutputStream forward = new PipedOutputStream(data);

                // Step 3 // Parse the SubData Packet Formatting
                new Thread(() -> read(this, reset, data), "SubDataClient::Packet_Listener(" + socket.getLocalSocketAddress().toString() + ')').start();

                // Step 2 // Decrypt the Data
                cipher.decrypt(this, raw, forward);
                forward.close();

            } catch (Exception e) {
                if (!reset.value) try {
                    if (!(e instanceof SocketException && !Boolean.getBoolean("subdata.debug"))) {
                        DebugUtil.logException(e, log);
                    } if (!(e instanceof SocketException)) {
                        if (e instanceof EncryptionException)
                            close(ENCRYPTION_MISMATCH);  // Classes that extend EncryptionException being thrown signify an ENCRYPTION_MISMATCH
                        else close(UNHANDLED_EXCEPTION);
                    } else close(CONNECTION_INTERRUPTED);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }, "SubDataClient::Data_Listener(" + socket.getLocalSocketAddress().toString() + ')').start();
    }

    private void write(SubDataSender sender, PacketOut next, OutputStream data) {
        // Step 1 // Create a detached data forwarding OutputStream
        try {
            PrimitiveContainer<Boolean> open = new PrimitiveContainer<>(true);
            OutputStream forward = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    if (open.value) data.write(b);
                }

                @Override
                public void close() throws IOException {
                    open.value = false;
                    Util.isException(data::close);
                }
            };
            // Step 2 // Write the Packet Metadata
            HashMap<Class<? extends PacketOut>, Integer> pOut = (state.asInt() >= POST_INITIALIZATION.asInt())?protocol.pOut:Util.reflect(InitialProtocol.class.getDeclaredField("pOut"), null);
            if (!pOut.keySet().contains(next.getClass())) throw new IllegalMessageException(getAddress().toString() + ": Could not find ID for packet: " + next.getClass().getCanonicalName());
            if (next.version() > 65535 || next.version() < 0) throw new IllegalMessageException(getAddress().toString() + ": Packet version is not in range (0x0000 to 0xFFFF): " + next.getClass().getCanonicalName());

            data.write(UnsignedDataHandler.toUnsigned((long) pOut.get(next.getClass()), 2));
            data.write(UnsignedDataHandler.toUnsigned((long) next.version(), 2));
            data.flush();

            // Step 3 // Invoke the Packet
            scheduler.run(() -> {
                try {
                    next.sending(sender);

                    if (next instanceof PacketStreamOut) {
                        ((PacketStreamOut) next).send(sender, forward);
                    } else forward.close();
                } catch (Throwable e) {
                    DebugUtil.logException(new InvocationTargetException(e, getAddress().toString() + ": Exception while running packet writer"), log);
                    Util.isException(forward::close);
                }
            });
            if (sender == this) while (open.value) Thread.sleep(125);
        } catch (Throwable e) {
            DebugUtil.logException(e, log);
            Util.isException(data::close);
        }
    }
    void write() {
        if (queue != null) new Thread(() -> {
            if (queue.size() > 0) {
                try {
                    PacketOut next = Util.getDespiteException(() -> queue.get(0), null);
                    Util.isException(() -> queue.remove(0));
                    if (next != null) {
                        if (!isClosed() || !(next instanceof PacketDisconnect || next instanceof PacketDisconnectUnderstood)) { // Disallows Disconnect packets during CLOSED
                            if (!isClosed() && (state != CLOSING || next instanceof PacketDisconnect || next instanceof PacketDisconnectUnderstood)) { // Allows only Disconnect packets during CLOSING
                                PipedOutputStream data = new PipedOutputStream();
                                PipedInputStream forward = new PipedInputStream(data, 1024);
                                new Thread(() -> write(this, next, data), "SubDataClient::Packet_Writer(" + socket.getLocalSocketAddress().toString() + ')').start();

                                // Step 5 // Add Escapes to the Encrypted Data
                                OutputStream raw = new OutputStream() {
                                    boolean open = true;

                                    @Override
                                    public void write(int b) throws IOException {
                                        if (open) {
                                            try {
                                                out.write(b);
                                            } catch (Throwable e) {
                                                close();
                                            }
                                        }
                                    }

                                    @Override
                                    public void close() throws IOException {
                                        if (open) {
                                            open = false;
                                            if (!socket.isClosed()) {
                                                out.control('\u0017');
                                                out.flush();
                                                if (queue != null && queue.size() > 0) SubDataClient.this.write();
                                                else queue = null;
                                            }
                                        }
                                    }
                                };

                                // Step 4 // Encrypt the Data
                                cipher.encrypt(this, forward, raw);
                                raw.close();
                                forward.close();
                            } else {
                                // Re-queue any pending packets during the CLOSING state
                                sendPacket(next);
                            }
                        }
                    }
                } catch (Throwable e) {
                    Util.isException(() -> queue.remove(0));
                    if (!(e instanceof SocketException)) { // Cut the write session short when socket issues occur
                        DebugUtil.logException(e, log);

                        if (queue != null && queue.size() > 0) SubDataClient.this.write();
                        else queue = null;
                    } else queue = null;
                }
            } else queue = null;
        }, "SubDataClient::Data_Writer(" + socket.getLocalSocketAddress().toString() + ')').start();
    }

    /**
     * Send a packet to Client
     *
     * @param packets Packets to send
     * @see ForwardOnly Packets must <b><u>NOT</u></b> be tagged as Forward-Only
     */
    public synchronized void sendPacket(PacketOut... packets) {
        List<PacketOut> list = new ArrayList<>();

        for (PacketOut packet : packets) {
            if (Util.isNull(packet)) throw new NullPointerException();
            if (packet instanceof ForwardOnly) throw new IllegalPacketException("Packet is Forward-Only");
            if (isClosed() || (state == CLOSING && !(packet instanceof PacketDisconnect || packet instanceof PacketDisconnectUnderstood))) {
                if (next == null) sendPacketLater(packet, CLOSED);
                else next.sendPacket(packet);
            } else if (state.asInt() < POST_INITIALIZATION.asInt() && !(packet instanceof InitialProtocol.Packet)) {
                sendPacketLater(packet, (packet instanceof InitialPacket)?POST_INITIALIZATION:READY);
            } else if (state == POST_INITIALIZATION && !(packet instanceof InitialPacket)) {
                sendPacketLater(packet, READY);
            } else {
                list.add(packet);
            }
        }

        if (list.size() > 0) {
            boolean init = false;

            if (queue == null) {
                queue = new LinkedList<>();
                init = true;
            }
            queue.addAll(list);

            if (init) write();
        }
    }
    private void sendPacketLater(PacketOut packet, ConnectionState state) {
        LinkedList<PacketOut> prequeue = (this.statequeue.keySet().contains(state))?this.statequeue.get(state):new LinkedList<PacketOut>();
        prequeue.add(packet);
        this.statequeue.put(state, prequeue);
    }

    /**
     * Forward a packet to another Client
     *
     * @param id Client ID
     * @param packets Packets to send
     * @see net.ME1312.SubData.Client.Protocol.Forwardable Packets must be tagged as Forwardable
     */
    public void forwardPacket(UUID id, PacketOut... packets) {
        List<PacketOut> list = new ArrayList<>();
        for (PacketOut packet : packets) {
            if (Util.isNull(id, packet)) throw new NullPointerException();
            if (!(packet instanceof Forwardable)) throw new IllegalPacketException("Packet is not Forwardable");
            sendPacket(new PacketForwardPacket(id, packet));
        }
        sendPacket(list.toArray(new PacketOut[0]));
    }

    public void sendMessage(MessageOut... messages) {
        List<PacketOut> list = new ArrayList<>();
        for (MessageOut message : messages) {
            if (Util.isNull(message)) throw new NullPointerException();
            if (message instanceof ForwardOnly) throw new IllegalMessageException("Message is Forward-Only");
            list.add(new PacketSendMessage(message));
        }
        sendPacket(list.toArray(new PacketOut[0]));
    }

    public void forwardMessage(UUID id, MessageOut... messages) {
        if (Util.isNull(id)) throw new NullPointerException();

        List<PacketOut> list = new ArrayList<>();
        for (MessageOut message : messages) {
            if (Util.isNull(message)) throw new NullPointerException();
            if (!(message instanceof Forwardable)) throw new IllegalMessageException("Message is not Forwardable");
            list.add(new PacketForwardPacket(id, new PacketSendMessage(message)));
        }
        sendPacket(list.toArray(new PacketOut[0]));
    }

    @Override
    public void getClient(UUID id, Callback<ObjectMap<String>> callback) {
        if (Util.isNull(id, callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        sendPacket(new PacketDownloadClientList(id, data -> {
            ObjectMap<String> serialized = null;
            if (data.contains(id.toString())) {
                serialized = data.getMap(id.toString());
            }

            try {
                callback.run(serialized);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    @Override
    public void getClients(Callback<Map<UUID, ObjectMap<String>>> callback) {
        if (Util.isNull(callback)) throw new NullPointerException();
        StackTraceElement[] origin = new Exception().getStackTrace();
        sendPacket(new PacketDownloadClientList(data -> {
            HashMap<UUID, ObjectMap<String>> serialized = new HashMap<UUID, ObjectMap<String>>();
            for (String id : data.getKeys()) {
                serialized.put(UUID.fromString(id), data.getMap(id));
            }

            try {
                callback.run(serialized);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    @Override
    public void ping(Callback<PingResponse> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        sendPacket(new PacketPing(response));
    }

    @Override
    public void ping(UUID id, Callback<PingResponse> response) {
        if (Util.isNull(response)) throw new NullPointerException();
        forwardPacket(id, new PacketPing(response));
    }

    /**
     * Get the underlying Client Socket
     *
     * @return Client Socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Get the Client that connects the Server to us
     *
     * @deprecated The Client connection to the Server is this
     * @return This Client
     */
    @Override
    @Deprecated
    public SubDataClient getConnection() {
        return this;
    }

    public SubDataProtocol getProtocol() {
        return protocol;
    }

    public InetSocketAddress getAddress() {
        return new InetSocketAddress(socket.getInetAddress(), socket.getPort());
    }

    /**
     * Get SubData's Block Size
     *
     * @return Block Size
     */
    public long getBlockSize() {
        return bs.get();
    }

    /**
     * Set SubData's Block Size
     *
     * @param size Block Size (null for super)
     */
    public void setBlockSize(Long size) {
        if (size == null) {
            bs = protocol.bs;
        } else bs = new Container<>(size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SubDataClient openChannel() throws IOException {
        return protocol.sub((Callback<Runnable>) constructor[0], (Logger) constructor[1], (InetAddress) constructor[2], (int) constructor[3], (ObjectMap<?>) constructor[4]);
    }

    /**
     * Reconnect the data stream using another Client
     *
     * @param client Client
     */
    public void reconnect(SubDataClient client) {
        if (Util.isNull(client)) throw new NullPointerException();
        if (client == this) throw new IllegalArgumentException("Cannot reconnect to 'this'");
        if (state.asInt() < CLOSING.asInt() || next != null) throw new IllegalStateException("Cannot override existing data stream");

        next = client;
        if (statequeue.keySet().contains(CLOSED)) {
            for (PacketOut packet : statequeue.get(CLOSED)) next.sendPacket(packet);
            statequeue.remove(CLOSED);
        }
    }

    public void close() throws IOException {
        if (state.asInt() < CLOSING.asInt() && !socket.isClosed()) {
            boolean result = true;
            LinkedList<ReturnCallback<DataClient, Boolean>> events = new LinkedList<>(on.close);
            for (ReturnCallback<DataClient, Boolean> next : events) try {
                if (next != null) result = next.run(this) != Boolean.FALSE && result;
            } catch (Throwable e) {
                DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), log);
            }

            if (result) {
                state = CLOSING;
                if (!isClosed()) sendPacket(new PacketDisconnect());

                Timer timeout = new Timer("SubDataClient::Disconnect_Timeout(" + socket.getLocalSocketAddress().toString() + ')');
                timeout.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (!socket.isClosed()) try {
                            close(CLOSE_REQUESTED);
                        } catch (IOException e) {
                            DebugUtil.logException(e, log);
                        }
                    }
                }, 5000);
            }
        }
    }

    void close(DisconnectReason reason) throws IOException {
        if (state != CLOSED && !socket.isClosed()) {
            if (state == CLOSING && reason == CONNECTION_INTERRUPTED) reason = CLOSE_REQUESTED;
            if (isdcr != null && reason == CONNECTION_INTERRUPTED) reason = isdcr;
            state = CLOSED;
            if (reason != CLOSE_REQUESTED) {
                log.warning("Disconnected from " + socket.getRemoteSocketAddress() + ": " + reason);
            } else log.info("Disconnected from " + socket.getRemoteSocketAddress());

            socket.close();
            cipher.retire(this);

            final DisconnectReason freason = reason;
            scheduler.run(() -> {
                LinkedList<Callback<NamedContainer<DisconnectReason, DataClient>>> events = new LinkedList<>(on.closed);
                for (Callback<NamedContainer<DisconnectReason, DataClient>> next : events) try {
                    if (next != null) next.run(new NamedContainer<>(freason, this));
                } catch (Throwable e) {
                    DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), log);
                }
            });
        }
    }

    public boolean isClosed() {
        return socket.isClosed();
    }
}
