package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Callback.ReturnCallback;
import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Encryption.NEH;
import net.ME1312.SubData.Server.Library.*;
import net.ME1312.SubData.Server.Library.Exception.EncryptionException;
import net.ME1312.SubData.Server.Library.Exception.IllegalMessageException;
import net.ME1312.SubData.Server.Library.Exception.IllegalPacketException;
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

import static net.ME1312.SubData.Server.Library.ConnectionState.*;
import static net.ME1312.SubData.Server.Library.DisconnectReason.*;

/**
 * SubData Client Class
 */
public class SubDataClient extends DataClient {
    private Socket socket;
    private InetSocketAddress address;
    private ClientHandler handler;
    private InputStreamL1 in;
    private OutputStreamL1 out;
    private ExecutorService writer;
    private HashMap<ConnectionState, LinkedList<PacketOut>> statequeue;
    private Thread read;
    private Cipher cipher = NEH.get();
    private int cipherlevel = 0;
    private SubDataServer subdata;
    private int bs;
    private SubDataClient next;
    private ConnectionState state;
    private DisconnectReason isdcr;
    private Timer timeout, heartbeat;
    private Object asr;

    SubDataClient(SubDataServer subdata, Socket client) throws IOException {
        if (Util.isNull(subdata, client)) throw new NullPointerException();
        this.subdata = subdata;
        this.bs = subdata.protocol.bs;
        state = PRE_INITIALIZATION;
        socket = client;
        in = new InputStreamL1(new BufferedInputStream(client.getInputStream(), bs), () -> close(CONNECTION_INTERRUPTED));
        out = new OutputStreamL1(subdata.log, client.getOutputStream(), bs, () -> close(CONNECTION_INTERRUPTED));
        writer = Executors.newSingleThreadExecutor(r -> new Thread(r, "SubDataServer::Data_Writer(" + address.toString() + ')'));
        statequeue = new HashMap<>();
        address = new InetSocketAddress(client.getInetAddress(), client.getPort());
        isdcr = PROTOCOL_MISMATCH;
        Timer timeout = this.timeout = new Timer("SubDataServer::Handshake_Timeout(" + address.toString() + ')');
        timeout.schedule(new TimerTask() {
            @Override
            public void run() {
                if (state.asInt() < POST_INITIALIZATION.asInt()) {
                    close(INITIALIZATION_TIMEOUT);
                }
                timeout.cancel();
            }
        }, subdata.timeout.value());
        heartbeat = new Timer("SubDataServer::Connection_Heartbeat(" + address.toString() + ')');
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
                Container<Boolean> open = new Container<>(true);
                InputStream forward = new InputStream() {
                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        if (open.value) {
                            int i = data.read(b, off, len);
                            if (i == -1) close();
                            return i;
                        } return -1;
                    }

                    @Override
                    public int read() throws IOException {
                        if (open.value) {
                            int b = data.read();
                            if (b == -1) close();
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
                    DebugUtil.logException(new IllegalStateException(getAddress().toString() + ": Only InitPacketDeclaration (0x0000) may be received during the PRE_INITIALIZATION stage: [" + DebugUtil.toHex(0xFFFF, id) + ", " + DebugUtil.toHex(0xFFFF, version) + "]"), subdata.log);
                    close(PROTOCOL_MISMATCH);
                } else if (state == CLOSING && id != 0xFFFE) {
                    forward.close(); // Suppress other packets during the CLOSING stage
                } else {
                    HashMap<Integer, PacketIn> pIn = (state.asInt() >= POST_INITIALIZATION.asInt())?subdata.protocol.pIn:Util.reflect(InitialProtocol.class.getDeclaredField("pIn"), null);
                    if (!pIn.keySet().contains(id)) throw new IllegalPacketException(getAddress().toString() + ": Could not find handler for packet: [" + DebugUtil.toHex(0xFFFF, id) + ", " + DebugUtil.toHex(0xFFFF, version) + "]");
                    PacketIn packet = pIn.get(id);
                    if (!packet.isCompatible(version)) throw new IllegalPacketException(getAddress().toString() + ": The handler does not support this packet version (" + DebugUtil.toHex(0xFFFF, packet.version()) + "): [" + DebugUtil.toHex(0xFFFF, id) + ", " + DebugUtil.toHex(0xFFFF, version) + "]");

                    // Step 5 // Invoke the Packet
                    if (state == PRE_INITIALIZATION && !(packet instanceof InitPacketDeclaration)) {
                        DebugUtil.logException(new IllegalStateException(getAddress().toString() + ": Only " + InitPacketDeclaration.class.getCanonicalName() + " may be received during the PRE_INITIALIZATION stage: " + packet.getClass().getCanonicalName()), subdata.log);
                        close(PROTOCOL_MISMATCH);
                    } else if (state == CLOSING && !(packet instanceof PacketDisconnectUnderstood)) {
                        forward.close(); // Suppress other packets during the CLOSING stage
                    } else {
                        subdata.scheduler.run(() -> {
                            try {
                                packet.receive(this);

                                if (packet instanceof PacketStreamIn) {
                                    ((PacketStreamIn) packet).receive(this, forward);
                                } else forward.close();
                            } catch (Throwable e) {
                                DebugUtil.logException(new InvocationTargetException(e, getAddress().toString() + ": Exception while running packet handler"), subdata.log);
                                Util.isException(forward::close);

                                if (state.asInt() <= INITIALIZATION.asInt())
                                    Util.isException(() -> close(PROTOCOL_MISMATCH)); // Issues during the init stages are signs of a PROTOCOL_MISMATCH
                            }
                        });
                        while (open.value) Thread.sleep(125);
                    }
                }
            }
        } catch (InterruptedIOException e) {
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
                    } else Util.isException(() -> close(CONNECTION_INTERRUPTED));
                });
                
                PipedInputStream data = new PipedInputStream(1024);
                PipedOutputStream forward = new PipedOutputStream(data);
                (read = new Thread(() -> read(reset, data), "SubDataServer::Packet_Listener(" + address.toString() + ')')).start();

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
        }, "SubDataServer::Data_Listener(" + address.toString() + ')').start();
    }

    private void write(PacketOut next, OutputStream data) {
        // Step 1 // Create a detached data forwarding OutputStream
        try {
            Container<Boolean> open = new Container<Boolean>(true);
            OutputStream forward = new OutputStream() {
                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    if (open.value) data.write(b, off, len);
                }

                @Override
                public void write(int b) throws IOException {
                    if (open.value) data.write(b);
                }

                @Override
                public void close() throws IOException {
                    open.value = false;
                    data.close();
                }
            };
            // Step 2 // Write the Packet Metadata
            HashMap<Class<? extends PacketOut>, Integer> pOut = (state.asInt() >= POST_INITIALIZATION.asInt())?subdata.protocol.pOut:Util.reflect(InitialProtocol.class.getDeclaredField("pOut"), null);
            if (!pOut.keySet().contains(next.getClass())) throw new IllegalMessageException(getAddress().toString() + ": Could not find ID for packet: " + next.getClass().getCanonicalName());
            if (next.version() > 65535 || next.version() < 0) throw new IllegalMessageException(getAddress().toString() + ": Packet version is not in range (0x0000 to 0xFFFF): " + next.getClass().getCanonicalName());

            data.write(UnsignedDataHandler.toUnsigned((long) pOut.get(next.getClass()), 2));
            data.write(UnsignedDataHandler.toUnsigned((long) next.version(), 2));
            data.flush();

            // Step 3 // Invoke the Packet
            subdata.scheduler.run(() -> {
                try {
                    if (next instanceof PacketStreamOut) {
                        ((PacketStreamOut) next).send(this, forward);
                    } else forward.close();

                    next.sending(this);
                } catch (Throwable e) {
                    DebugUtil.logException(e, subdata.log);
                    Util.isException(forward::close);
                }
            });
            while (open.value) Thread.sleep(125);
        } catch (Throwable e) {
            DebugUtil.logException(e, subdata.log);
            Util.isException(data::close);
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
                            out.flush();
                            out.control('\u0017');
                        }
                    } catch (Throwable e) {
                        if (!(e instanceof SocketException)) {
                            DebugUtil.logException(e, subdata.log);
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
    public synchronized void sendPacket(PacketOut... packets) {
        for (PacketOut packet : packets) {
            if (Util.isNull(packet)) throw new NullPointerException();
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

    public void sendMessage(MessageOut... messages) {
        List<PacketOut> list = new ArrayList<>();
        for (MessageOut message : messages) {
            if (Util.isNull(message)) throw new NullPointerException();
            list.add(new PacketSendMessage(message));
        }
        sendPacket(list.toArray(new PacketOut[0]));
    }

    @Override
    public void ping(Callback<PingResponse> response) {
        if (Util.isNull(response)) throw new NullPointerException();
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
        out.resize(bs);
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
    public void newChannel(Callback<DataClient> client) {
        openChannel(client::run);
    }

    /**
     * Open an Async Data SubChannel
     *
     * @return New SubData Channel
     */
    public void openChannel(Callback<SubDataClient> client) {
        sendPacket(new PacketOpenChannel(client));
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

    public void close() {
        if (state.asInt() < CLOSING.asInt() && !socket.isClosed()) {
            boolean result = true;
            LinkedList<ReturnCallback<DataClient, Boolean>> events = new LinkedList<>(on.close);
            for (ReturnCallback<DataClient, Boolean> next : events) try {
                if (next != null) result = next.run(this) != Boolean.FALSE && result;
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
            if (read != null) read.interrupt();
            if (reason != CLOSE_REQUESTED) {
                subdata.log.warning(getAddress().toString() + " has disconnected: " + reason);
            } else subdata.log.info(getAddress().toString() + " has disconnected");

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
            if (subdata.getClients().values().contains(this)) subdata.removeClient(this);

            final DisconnectReason freason = reason;
            subdata.scheduler.run(() -> {
                LinkedList<Callback<Pair<DisconnectReason, DataClient>>> events = new LinkedList<>(on.closed);
                for (Callback<Pair<DisconnectReason, DataClient>> next : events) try {
                    if (next != null) next.run(new ContainedPair<>(freason, this));
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
