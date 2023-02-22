package net.ME1312.SubData.Client;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Encryption.NEH;
import net.ME1312.SubData.Client.Library.*;
import net.ME1312.SubData.Client.Library.Exception.*;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import static net.ME1312.SubData.Client.Library.ConnectionState.*;
import static net.ME1312.SubData.Client.Library.DisconnectReason.*;

/**
 * SubData Client Class
 */
public class SubDataClient extends DataClient implements SubDataSender {
    private Socket socket;
    private InetSocketAddress address;
    private InputStreamL1 in;
    private OutputStreamL1 out;
    private ExecutorService writer;
    private HashMap<ConnectionState, LinkedList<PacketOut>> statequeue;
    private Runnable readc;
    private int bs;
    private SubDataProtocol protocol;
    private Cipher cipher = NEH.get();
    private int cipherlevel = 0;
    private ObjectMap<?> login;
    private ConnectionState state;
    private DisconnectReason isdcr;
    private Consumer<Runnable> scheduler;
    private Object[] constructor;
    private SubDataClient next;
    private Logger log;
    private Timer heartbeat;

    SubDataClient(SubDataProtocol protocol, Consumer<Runnable> scheduler, Logger log, InetAddress address, int port, ObjectMap<?> login) throws IOException {
        Util.nullpo(address, port);
        this.protocol = protocol;
        this.bs = protocol.bs;
        this.login = login;
        this.scheduler = scheduler;
        this.log = log;
        this.state = PRE_INITIALIZATION;
        this.isdcr = PROTOCOL_MISMATCH;
        this.socket = new Socket(address, port);
        this.address = new InetSocketAddress(socket.getInetAddress(), socket.getPort());
        this.writer = Executors.newSingleThreadExecutor(r -> new Thread(r, "SubDataClient::Data_Writer(" + this.address.toString() + ')'));
        this.out = new OutputStreamL1(log, socket.getOutputStream(), bs, () -> close(CONNECTION_INTERRUPTED), "SubDataClient::Block_Writer(" + this.address.toString() + ')');
        this.in = new InputStreamL1(new BufferedInputStream(socket.getInputStream()), () -> close(CONNECTION_INTERRUPTED), e -> {
            DebugUtil.logException(new ProtocolException(this.address.toString() + ": Received invalid L1 control character: " + DebugUtil.toHex(0xFF, e)), log);
            close(PROTOCOL_MISMATCH);
        });
        this.statequeue = new HashMap<>();
        this.constructor = new Object[]{
                scheduler,
                log,
                address,
                port,
                login
        };
        heartbeat = new Timer("SubDataClient::Connection_Heartbeat(" + this.address.toString() + ')');
        heartbeat.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!writer.isShutdown()) writer.submit(() -> {
                    if (heartbeat != null) {
                        out.control('\u0000');
                        TimerTask action = this;
                        heartbeat.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                action.run();
                            }
                        }, 5000);
                    }
                });
            }
        }, 5000);

        log.info("Connected to " + socket.getRemoteSocketAddress());
        read();
    }

    private void read(SubDataSender sender, Container<Boolean> reset, InputStream data) {
        try {
            // Step 3 // Read the Packet Metadata
            byte[] pending = new byte[2];
            int id = -1;

            int b, i = 0;
            while ((b = data.read()) != -1) {
                pending[i] = (byte) b;
                if (++i == 2) {
                    id = (int) UnsignedData.resign(pending);
                    break;
                }
            }

            // Step 4 // Create a detached data forwarding InputStream
            if (state != CLOSED && id >= 0) {
                Container<Boolean> open = new Container<>(true);
                InputStream forward = new InputStream() {
                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        int i = data.read(b, off, len);
                        if (i == -1) open.value = false;
                        return i;
                    }

                    @Override
                    public int read() throws IOException {
                        int b = data.read();
                        if (b == -1) open.value = false;
                        return b;
                    }

                    @Override
                    public void close() throws IOException {
                        try {
                            while (data.read() != -1);
                        } finally {
                            open.value = false;
                        }
                    }
                };
                if (state == PRE_INITIALIZATION && id != 0x0000) {
                    throw new ProtocolException(address.toString() + ": Only 0x0000 may be received during the PRE_INITIALIZATION stage: [" + DebugUtil.toHex(0xFFFF, id) + "]");
                } else if (state == CLOSING && id != 0xFFFE) {
                    forward.close(); // Suppress other packets during the CLOSING stage
                } else {
                    HashMap<Integer, PacketIn> pIn = (state.asInt() >= POST_INITIALIZATION.asInt())?protocol.pIn:Util.reflect(InitialProtocol.class.getDeclaredField("pIn"), null);
                    if (!pIn.keySet().contains(id)) throw new IllegalPacketException(address.toString() + ": Could not find handler for packet: [" + DebugUtil.toHex(0xFFFF, id) + "]");
                    PacketIn packet = pIn.get(id);
                    if (sender instanceof ForwardedDataSender && !(packet instanceof Forwardable)) throw new IllegalSenderException(address.toString() + ": This handler does not support forwarded packets: [" + packet.getClass().getTypeName() + "]");
                    if (sender instanceof SubDataClient && packet instanceof ForwardOnly) throw new IllegalSenderException(address.toString() + ": This handler does not support non-forwarded packets: [" + packet.getClass().getTypeName() + "]");

                    // Step 5 // Invoke the Packet
                    if (state == PRE_INITIALIZATION && !(packet instanceof InitPacketDeclaration)) {
                        throw new ProtocolException(address.toString() + ": Only " + InitPacketDeclaration.class.getTypeName() + " may be received during the PRE_INITIALIZATION stage: [" + packet.getClass().getTypeName() + "]");
                    } else if (state == CLOSING && !(packet instanceof PacketDisconnectUnderstood)) {
                        forward.close(); // Suppress other packets during the CLOSING stage
                    } else {
                        readc = () -> open.value = false;
                        scheduler.accept(() -> {
                            try {
                                packet.receive(sender);

                                if (packet instanceof PacketStreamIn) {
                                    ((PacketStreamIn) packet).receive(sender, forward);
                                } else forward.close();
                            } catch (Throwable e) {
                                DebugUtil.logException(new InvocationTargetException(e, address.toString() + ": Exception while running packet handler"), log);
                                Try.all.run(forward::close);

                                if (state.asInt() <= INITIALIZATION.asInt())
                                    Try.all.run(() -> close(PROTOCOL_MISMATCH)); // Issues during the init stages are signs of a PROTOCOL_MISMATCH
                            }
                        });
                        if (sender == this) while (open.value) Thread.sleep(125);
                    }
                }
            }
        } catch (InterruptedIOException e) {
        } catch (ProtocolException e) {
            DebugUtil.logException(e, log);
            close(PROTOCOL_MISMATCH);
        } catch (Exception e) {
            if (!reset.value) {
                if (!(e instanceof SocketException && !Boolean.getBoolean("subdata.debug"))) {
                    DebugUtil.logException(e, log);
                } if (!(e instanceof SocketException)) {
                    close(UNHANDLED_EXCEPTION);
                } else close(CONNECTION_INTERRUPTED);
            }
        }
    }
    void read() {
        if (!isClosed()) new Thread(() -> {
            Container<Boolean> reset = new Container<>(false);
            if (!isClosed()) try {
                // Step 1 // Parse Escapes in the Encrypted Data
                InputStream raw = in.open(() -> {
                    if (state != PRE_INITIALIZATION) reset.value = true;
                }, () -> {
                    if (!isClosed()) {
                        SubDataClient.this.read();
                    } else Try.all.run(() -> close(CONNECTION_INTERRUPTED));
                });

                PipedInputStream data = new PipedInputStream(1024);
                PipedOutputStream forward = new PipedOutputStream(data);
                Thread reader = new Thread(() -> read(this, reset, data), "SubDataClient::Packet_Reader(" + address.toString() + ')');
                readc = reader::interrupt;
                reader.start();

                // Step 2 // Decrypt the Data
                cipher.decrypt(this, raw, forward);
                forward.close();

            } catch (Exception e) {
                if (!reset.value) {
                    if (!(e instanceof SocketException && !Boolean.getBoolean("subdata.debug"))) {
                        DebugUtil.logException(e, log);
                    } if (!(e instanceof SocketException)) {
                        if (e instanceof EncryptionException)
                            close(ENCRYPTION_MISMATCH);  // Classes that extend EncryptionException being thrown signify an ENCRYPTION_MISMATCH
                        else close(UNHANDLED_EXCEPTION);
                    } else close(CONNECTION_INTERRUPTED);
                }
            }
        }, "SubDataClient::Data_Reader(" + address.toString() + ')').start();
    }

    private void write(SubDataSender sender, PacketOut next, OutputStream data) {
        // Step 1 // Create a detached data forwarding OutputStream
        try {
            Container<Boolean> open = new Container<>(true);
            OutputStream forward = new OutputStream() {
                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    data.write(b, off, len);
                }

                @Override
                public void write(int b) throws IOException {
                    data.write(b);
                }

                @Override
                public void close() throws IOException {
                    open.value = false;
                    data.close();
                }
            };
            // Step 2 // Write the Packet Metadata
            HashMap<Class<? extends PacketOut>, Integer> pOut = (state.asInt() >= POST_INITIALIZATION.asInt())?protocol.pOut:Util.reflect(InitialProtocol.class.getDeclaredField("pOut"), null);
            if (!pOut.keySet().contains(next.getClass())) throw new IllegalMessageException(address.toString() + ": Could not find ID for packet: " + next.getClass().getTypeName());

            data.write(UnsignedData.unsign((long) pOut.get(next.getClass()), 2), 0, 2);
            data.flush();

            // Step 3 // Invoke the Packet
            scheduler.accept(() -> {
                try {
                    next.sending(sender);

                    if (next instanceof PacketStreamOut) {
                        ((PacketStreamOut) next).send(sender, forward);
                    } else forward.close();
                } catch (Throwable e) {
                    DebugUtil.logException(new InvocationTargetException(e, address.toString() + ": Exception while running packet writer"), log);
                    Try.all.run(forward::close);
                }
            });
            if (sender == this) while (open.value) Thread.sleep(125);
        } catch (Throwable e) {
            DebugUtil.logException(e, log);
            Try.all.run(data::close);
        }
    }
    void write(PacketOut packet) {
        if (packet != null) {
            if (!isClosed() || !(packet instanceof PacketDisconnect || packet instanceof PacketDisconnectUnderstood)) { // Disallows Disconnect packets during CLOSED
                if (!isClosed() && (state != CLOSING || packet instanceof PacketDisconnect || packet instanceof PacketDisconnectUnderstood)) { // Allows only Disconnect packets during CLOSING
                    try {
                        PipedOutputStream data = new PipedOutputStream();
                        PipedInputStream forward = new PipedInputStream(data, 1024);
                        new Thread(() -> write(this, packet, data), "SubDataClient::Packet_Writer(" + address.toString() + ')').start();

                        // Step 4 // Encrypt the Data
                        cipher.encrypt(this, forward, out);
                        forward.close();

                        // Step 5 // Add Escapes to the Encrypted Data
                        if (!socket.isClosed()) {
                            out.limit = bs; // Reset temp size
                            out.flush();
                            out.control('\u0017');
                        }
                    } catch (Throwable e) {
                        if (!(e instanceof SocketException)) {
                            DebugUtil.logException(e, log);
                        }
                    }
                } else {
                    // Re-queue any pending packets during the CLOSING state
                    sendPacket(packet);
                }
            }
        }
    }

    /**
     * Send a packet to Client
     *
     * @param packets Packets to send
     * @see ForwardOnly Packets must <b><u>NOT</u></b> be tagged as Forward-Only
     */
    public void sendPacket(PacketOut... packets) {
        for (PacketOut packet : packets) {
            if (Util.isNull(packet)) continue;
            if (packet instanceof ForwardOnly) throw new IllegalPacketException("Packet is Forward-Only");
            if (isClosed() || (state == CLOSING && !(packet instanceof PacketDisconnect || packet instanceof PacketDisconnectUnderstood))) {
                if (next == null) sendPacketLater(packet, CLOSED);
                else next.sendPacket(packet);
            } else if (state.asInt() < POST_INITIALIZATION.asInt() && !(packet instanceof InitialProtocol.Packet)) {
                sendPacketLater(packet, (packet instanceof InitialPacket)?POST_INITIALIZATION:READY);
            } else if (state == POST_INITIALIZATION && !(packet instanceof InitialPacket)) {
                sendPacketLater(packet, READY);
            } else if (!writer.isShutdown()) {
                writer.submit(() -> write(packet));
            }
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
            if (Util.isNull(id, packet)) continue;
            if (!(packet instanceof Forwardable)) throw new IllegalPacketException("Packet is not Forwardable");
            sendPacket(new PacketForwardPacket(id, packet));
        }
        sendPacket(list.toArray(new PacketOut[0]));
    }

    public void sendMessage(MessageOut... messages) {
        List<PacketOut> list = new ArrayList<>();
        for (MessageOut message : messages) {
            if (Util.isNull(message)) continue;
            if (message instanceof ForwardOnly) throw new IllegalMessageException("Message is Forward-Only");
            list.add(new PacketSendMessage(message));
        }
        sendPacket(list.toArray(new PacketOut[0]));
    }

    public void forwardMessage(UUID id, MessageOut... messages) {
        Util.nullpo(id);

        List<PacketOut> list = new ArrayList<>();
        for (MessageOut message : messages) {
            if (Util.isNull(message)) continue;
            if (!(message instanceof Forwardable)) throw new IllegalMessageException("Message is not Forwardable");
            list.add(new PacketForwardPacket(id, new PacketSendMessage(message)));
        }
        sendPacket(list.toArray(new PacketOut[0]));
    }

    @Override
    public void getClient(UUID id, Consumer<ObjectMap<String>> callback) {
        Util.nullpo(id, callback);
        StackTraceElement[] origin = new Exception().getStackTrace();
        sendPacket(new PacketDownloadClientList(id, data -> {
            ObjectMap<String> serialized = null;
            if (data.contains(id.toString())) {
                serialized = data.getMap(id.toString());
            }

            try {
                callback.accept(serialized);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    @Override
    public void getClients(Consumer<Map<UUID, ObjectMap<String>>> callback) {
        Util.nullpo(callback);
        StackTraceElement[] origin = new Exception().getStackTrace();
        sendPacket(new PacketDownloadClientList(data -> {
            HashMap<UUID, ObjectMap<String>> serialized = new HashMap<UUID, ObjectMap<String>>();
            for (String id : data.getKeys()) {
                serialized.put(UUID.fromString(id), data.getMap(id));
            }

            try {
                callback.accept(serialized);
            } catch (Throwable e) {
                Throwable ew = new InvocationTargetException(e);
                ew.setStackTrace(origin);
                ew.printStackTrace();
            }
        }));
    }

    @Override
    public void ping(Consumer<PingResponse> response) {
        Util.nullpo(response);
        sendPacket(new PacketPing(response));
    }

    @Override
    public void ping(UUID id, Consumer<PingResponse> response) {
        Util.nullpo(response);
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
        return address;
    }

    /**
     * Get SubData's Block Size
     *
     * @return Block Size
     */
    public int getBlockSize() {
        return bs;
    }

    /**
     * Set SubData's Block Size
     *
     * @param size Block Size (null for super)
     */
    public void setBlockSize(Integer size) {
        if (size == null) {
            bs = protocol.bs;
        } else bs = size;
        out.limit = bs;
    }

    /**
     * Set SubData's Block Size for the current packet
     *
     * @param size Block Size (null for default)
     */
    @Override
    public void tempBlockSize(Integer size) {
        out.resize((size == null)? bs : size);
    }

    @Override
    @Deprecated
    public DataClient newChannel() throws IOException {
        return openChannel();
    }

    /**
     * Open an Async Data SubChannel
     *
     * @return New SubData Channel
     */
    @SuppressWarnings("unchecked")
    public SubDataClient openChannel() throws IOException {
        return protocol.sub((Consumer<Runnable>) constructor[0], (Logger) constructor[1], (InetAddress) constructor[2], (int) constructor[3], (ObjectMap<?>) constructor[4]);
    }

    /**
     * Reconnect the data stream using another Client
     *
     * @param client Client
     */
    public void reconnect(SubDataClient client) {
        Util.nullpo(client);
        if (client == this) throw new IllegalArgumentException("Cannot reconnect to 'this'");
        if (state.asInt() < CLOSING.asInt() || next != null) throw new IllegalStateException("Cannot override existing data stream");

        next = client;
        if (statequeue.keySet().contains(CLOSED)) {
            for (PacketOut packet : statequeue.get(CLOSED)) next.sendPacket(packet);
            statequeue.remove(CLOSED);
        }
    }

    public void close() {
        if (state.asInt() < CLOSING.asInt() && !socket.isClosed()) {
            boolean result = true;
            LinkedList<Function<DataClient, Boolean>> events = new LinkedList<>(on.close);
            for (Function<DataClient, Boolean> next : events) try {
                if (next != null) result = next.apply(this) != Boolean.FALSE && result;
            } catch (Throwable e) {
                DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), log);
            }

            if (result) {
                state = CLOSING;
                if (!isClosed()) sendPacket(new PacketDisconnect());

                Timer timeout = new Timer("SubDataClient::Disconnect_Timeout(" + address.toString() + ')');
                timeout.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (!socket.isClosed()) close(CLOSE_REQUESTED);
                        timeout.cancel();
                    }
                }, 5000);
            }
        }
    }

    void close(DisconnectReason reason) {
        if (state != CLOSED) {
            if (state == CLOSING && reason == CONNECTION_INTERRUPTED) reason = CLOSE_REQUESTED;
            else if (isdcr != null && reason == CONNECTION_INTERRUPTED) reason = isdcr;

            state = CLOSED;
            if (readc != null) readc.run();
            if (reason != CLOSE_REQUESTED) {
                log.warning("Disconnected from " + socket.getRemoteSocketAddress() + ": " + reason);
            } else log.info("Disconnected from " + socket.getRemoteSocketAddress());

            heartbeat.cancel();
            heartbeat = null;
            writer.shutdown();
            out.shutdown();
            in.shutdown();
            try {
                socket.close();
            } catch (IOException e) {
                DebugUtil.logException(e, log);
            }
            cipher.retire(this);

            final DisconnectReason freason = reason;
            scheduler.accept(() -> {
                LinkedList<Consumer<Pair<DisconnectReason, DataClient>>> events = new LinkedList<>(on.closed);
                for (Consumer<Pair<DisconnectReason, DataClient>> next : events) try {
                    if (next != null) next.accept(new ContainedPair<>(freason, this));
                } catch (Throwable e) {
                    DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), log);
                }
            });
        }
    }

    public boolean isClosed() {
        return state == CLOSED || socket.isClosed();
    }
}
