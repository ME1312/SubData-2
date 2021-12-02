package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Try;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Encryption.NEH;
import net.ME1312.SubData.Server.Library.*;
import net.ME1312.SubData.Server.Library.Exception.EncryptionException;
import net.ME1312.SubData.Server.Library.Exception.IllegalMessageException;
import net.ME1312.SubData.Server.Library.Exception.IllegalPacketException;
import net.ME1312.SubData.Server.Library.Exception.ProtocolException;
import net.ME1312.SubData.Server.Protocol.Initial.InitPacketDeclaration;
import net.ME1312.SubData.Server.Protocol.Initial.InitialPacket;
import net.ME1312.SubData.Server.Protocol.Initial.InitialProtocol;
import net.ME1312.SubData.Server.Protocol.Internal.*;
import net.ME1312.SubData.Server.Protocol.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.ME1312.SubData.Server.Library.ConnectionState.*;
import static net.ME1312.SubData.Server.Library.DisconnectReason.*;

/**
 * SubData Client Class
 */
public class SubDataClient extends DataClient {
    private final Socket socket;
    private final InetSocketAddress address;
    private ClientHandler handler;
    private final InputStreamL1 in;
    private final OutputStreamL1 out;
    private final ExecutorService writer;
    private final HashMap<ConnectionState, LinkedList<PacketOut>> statequeue;
    private Runnable readc;
    private final Cipher cipher = NEH.get();
    private final int cipherlevel = 0;
    private final SubDataServer subdata;
    private int bs;
    private SubDataClient next;
    private ConnectionState state;
    private final DisconnectReason isdcr;
    private Timer timeout, heartbeat;
    private Object asr;

    SubDataClient(SubDataServer subdata, Socket client) throws IOException {
        Util.nullpo(subdata, client);
        this.subdata = subdata;
        this.bs = subdata.protocol.bs;
        state = PRE_INITIALIZATION;
        socket = client;
        address = new InetSocketAddress(client.getInetAddress(), client.getPort());
        writer = Executors.newSingleThreadExecutor(r -> new Thread(r, "SubDataServer::Data_Writer(" + address + ')'));
        out = new OutputStreamL1(subdata.log, client.getOutputStream(), bs, () -> close(CONNECTION_INTERRUPTED), "SubDataServer::Block_Writer(" + address + ')');
        in = new InputStreamL1(new BufferedInputStream(client.getInputStream()), () -> close(CONNECTION_INTERRUPTED), e -> {
            DebugUtil.logException(new ProtocolException(address + ": Received invalid L1 control character: " + DebugUtil.toHex(0xFF, e)), subdata.log);
            close(PROTOCOL_MISMATCH);
        });
        statequeue = new HashMap<>();
        isdcr = PROTOCOL_MISMATCH;
        Timer timeout = this.timeout = new Timer("SubDataServer::Handshake_Timeout(" + address + ')');
        timeout.schedule(new TimerTask() {
            @Override
            public void run() {
                if (state.asInt() < POST_INITIALIZATION.asInt()) {
                    close(INITIALIZATION_TIMEOUT);
                }
                timeout.cancel();
            }
        }, subdata.timeout.value());
        heartbeat = new Timer("SubDataServer::Connection_Heartbeat(" + address + ')');
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
    }

    private void read(Container<Boolean> reset, InputStream data) {
        try {
            // Step 3 // Read the Packet Metadata
            byte[] pending = new byte[2];
            int id = -1;

            int b, position = 0;
            while (position < 2 && (b = data.read()) != -1) {
                pending[position] = (byte) b;
                if (++position == 2) {
                    id = (int) UnsignedData.resign(pending);
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
                    HashMap<Integer, PacketIn> pIn = (state.asInt() >= POST_INITIALIZATION.asInt())?subdata.protocol.pIn:Util.reflect(InitialProtocol.class.getDeclaredField("pIn"), null);
                    if (!pIn.containsKey(id)) throw new IllegalPacketException(address.toString() + ": Could not find handler for packet: [" + DebugUtil.toHex(0xFFFF, id) + "]");
                    PacketIn packet = pIn.get(id);

                    // Step 5 // Invoke the Packet
                    if (state == PRE_INITIALIZATION && !(packet instanceof InitPacketDeclaration)) {
                        throw new ProtocolException(address.toString() + ": Only " + InitPacketDeclaration.class.getCanonicalName() + " may be received during the PRE_INITIALIZATION stage: [" + packet.getClass().getCanonicalName() + "]");
                    } else if (state == CLOSING && !(packet instanceof PacketDisconnectUnderstood)) {
                        forward.close(); // Suppress other packets during the CLOSING stage
                    } else {
                        readc = () -> open.value = false;
                        subdata.scheduler.accept(() -> {
                            try {
                                packet.receive(this);

                                if (packet instanceof PacketStreamIn) {
                                    ((PacketStreamIn) packet).receive(this, forward);
                                } else forward.close();
                            } catch (Throwable e) {
                                DebugUtil.logException(new InvocationTargetException(e, address.toString() + ": Exception while running packet handler"), subdata.log);
                                Try.all.run(forward::close);

                                if (state.asInt() <= INITIALIZATION.asInt())
                                    Try.all.run(() -> close(PROTOCOL_MISMATCH)); // Issues during the init stages are signs of a PROTOCOL_MISMATCH
                            }
                        });
                        while (open.value) Thread.sleep(125);
                    }
                }
            }
        } catch (InterruptedIOException ignored) {
        } catch (ProtocolException e) {
            DebugUtil.logException(e, subdata.log);
            close(PROTOCOL_MISMATCH);
        } catch (Exception e) {
            if (!reset.value) {
                if (!(e instanceof SocketException && !Boolean.getBoolean("subdata.debug"))) {
                    DebugUtil.logException(e, subdata.log);
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
                Thread reader = new Thread(() -> read(reset, data), "SubDataServer::Packet_Reader(" + address.toString() + ')');
                readc = reader::interrupt;
                reader.start();

                // Step 2 // Decrypt the Data
                cipher.decrypt(this, raw, forward);
                forward.close();

            } catch (Exception e) {
                if (!reset.value) {
                    if (!(e instanceof SocketException && !Boolean.getBoolean("subdata.debug"))) {
                        DebugUtil.logException(e, subdata.log);
                    } if (!(e instanceof SocketException)) {
                        if (e instanceof EncryptionException)
                            close(ENCRYPTION_MISMATCH);  // Classes that extend EncryptionException being thrown signify an ENCRYPTION_MISMATCH
                        else close(UNHANDLED_EXCEPTION);
                    } else close(CONNECTION_INTERRUPTED);
                }
            }
        }, "SubDataServer::Data_Reader(" + address.toString() + ')').start();
    }

    private void write(PacketOut next, OutputStream data) {
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
            HashMap<Class<? extends PacketOut>, Integer> pOut = (state.asInt() >= POST_INITIALIZATION.asInt())?subdata.protocol.pOut:Util.reflect(InitialProtocol.class.getDeclaredField("pOut"), null);
            if (!pOut.containsKey(next.getClass())) throw new IllegalMessageException(address.toString() + ": Could not find ID for packet: " + next.getClass().getCanonicalName());

            data.write(UnsignedData.unsign((long) pOut.get(next.getClass()), 2), 0, 2);
            data.flush();

            // Step 3 // Invoke the Packet
            subdata.scheduler.accept(() -> {
                try {
                    next.sending(this);

                    if (next instanceof PacketStreamOut) {
                        ((PacketStreamOut) next).send(this, forward);
                    } else forward.close();
                } catch (Throwable e) {
                    DebugUtil.logException(new InvocationTargetException(e, address.toString() + ": Exception while running packet writer"), subdata.log);
                    Try.all.run(forward::close);
                }
            });
            while (open.value) Thread.sleep(125);
        } catch (Throwable e) {
            DebugUtil.logException(e, subdata.log);
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
                        new Thread(() -> write(packet, data), "SubDataServer::Packet_Writer(" + address.toString() + ')').start();

                        // Step 4 // Encrypt the Data
                        cipher.encrypt(this, forward, out);
                        forward.close();

                        // Step 5 // Add Escapes to the Encrypted Data
                        if (!socket.isClosed()) {
                            out.limit = bs;
                            out.flush();
                            out.control('\u0017');
                        }
                    } catch (Throwable e) {
                        if (!(e instanceof SocketException)) {
                            DebugUtil.logException(new InvocationTargetException(e, address.toString() + ": Exception while running packet writer"), subdata.log);
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
     * Send a packet to the Client
     *
     * @param packets Packets to send
     */
    public void sendPacket(PacketOut... packets) {
        for (PacketOut packet : packets) {
            if (Util.isNull(packet)) continue;
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
        LinkedList<PacketOut> prequeue = (this.statequeue.containsKey(state))?this.statequeue.get(state):new LinkedList<PacketOut>();
        prequeue.add(packet);
        this.statequeue.put(state, prequeue);
    }

    public void sendMessage(MessageOut... messages) {
        List<PacketOut> list = new ArrayList<>();
        for (MessageOut message : messages) {
            if (Util.isNull(message)) continue;
            list.add(new PacketSendMessage(message));
        }
        sendPacket(list.toArray(new PacketOut[0]));
    }

    @Override
    public void ping(Consumer<PingResponse> response) {
        Util.nullpo(response);
        sendPacket(new PacketPing(response));
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
     * Get the Server this Client belongs to
     *
     * @return SubData Server
     */
    public SubDataServer getServer() {
        return subdata;
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
            bs = subdata.protocol.bs;
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

    public Object getAuthResponse() {
        return asr;
    }

    public ClientHandler getHandler() {
        return handler;
    }

    /**
     * Sets the Handler (should only be called by Handlers themselves)
     *
     * @see ClientHandler
     * @param obj Handler
     */
    public void setHandler(ClientHandler obj) {
        if (handler != null && Arrays.asList(handler.getSubData()).contains(this)) handler.removeSubData(this);
        handler = obj;
    }

    @Override
    @Deprecated
    public void newChannel(Consumer<DataClient> client) {
        openChannel(client::accept);
    }

    /**
     * Open an Async Data SubChannel
     *
     * @return New SubData Channel
     */
    public void openChannel(Consumer<SubDataClient> client) {
        sendPacket(new PacketOpenChannel(client));
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
        if (statequeue.containsKey(CLOSED)) {
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
                DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), subdata.log);
            }

            if (result) {
                state = CLOSING;
                if (!isClosed()) sendPacket(new PacketDisconnect());

                timeout = new Timer("SubDataServer::Disconnect_Timeout(" + address.toString() + ')');
                timeout.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        close(CLOSE_REQUESTED);
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
            timeout.cancel();
            if (readc != null) readc.run();
            if (reason != CLOSE_REQUESTED) {
                subdata.log.warning(address.toString() + " has disconnected: " + reason);
            } else subdata.log.info(address.toString() + " has disconnected");

            heartbeat.cancel();
            heartbeat = null;
            writer.shutdown();
            out.shutdown();
            in.shutdown();
            try {
                socket.close();
            } catch (IOException e) {
                DebugUtil.logException(e, subdata.log);
            }
            cipher.retire(this);
            if (handler != null) {
                ClientHandler tmp = handler;
                setHandler(null);
                handler = tmp;
            }
            if (subdata.getClients().containsValue(this)) subdata.removeClient(this);

            final DisconnectReason freason = reason;
            subdata.scheduler.accept(() -> {
                LinkedList<Consumer<Pair<DisconnectReason, DataClient>>> events = new LinkedList<>(on.closed);
                for (Consumer<Pair<DisconnectReason, DataClient>> next : events) try {
                    if (next != null) next.accept(new ContainedPair<>(freason, this));
                } catch (Throwable e) {
                    DebugUtil.logException(new InvocationTargetException(e, "Unhandled exception while running SubData Event"), subdata.log);
                }
            });
        }
    }

    public boolean isClosed() {
        return state == CLOSED || socket.isClosed();
    }
}
