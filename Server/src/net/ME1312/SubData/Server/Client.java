package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Library.Exception.IllegalMessageException;
import net.ME1312.SubData.Server.Library.Exception.IllegalPacketException;
import net.ME1312.SubData.Server.Protocol.*;
import net.ME1312.SubData.Server.Protocol.Internal.PacketSendMessage;
import org.msgpack.core.MessageInsufficientBufferException;
import org.msgpack.core.MessagePack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import static net.ME1312.SubData.Server.SubDataServer.pIn;
import static net.ME1312.SubData.Server.SubDataServer.pOut;

/**
 * Network Client Class
 */
public class Client {
    private Socket socket;
    private InetSocketAddress address;
    private ClientHandler handler;
    private LinkedList<PacketOut> queue;
    private OutputStream out;
    private Timer ready;
    private SubDataServer subdata;
    boolean closed;

    /**
     * Network Client
     *
     * @param subdata SubData Direct Server
     * @param client Socket to Bind
     */
    public Client(SubDataServer subdata, Socket client) throws IOException {
        if (Util.isNull(subdata, client)) throw new NullPointerException();
        this.subdata = subdata;
        closed = false;
        socket = client;
        out = client.getOutputStream();
        queue = null;
        address = new InetSocketAddress(client.getInetAddress(), client.getPort());
        ready = null;
        /*ready = new Timer("SubDataServer::Handshake_Timeout(" + address.toString() + ')');
        ready.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!socket.isClosed()) try {
                    subdata.removeClient(Client.this);
                } catch (IOException e) {
                    SubDataServer.log.error.println(e);
                }
            }
        }, 15000);*/
        read();
    }

    private void read() {
        new Thread(() -> {
            try {
                InputStream data = socket.getInputStream();
                ByteArrayOutputStream pending = new ByteArrayOutputStream();
                int id = -1, version = -1;

                int b, position = 0;
                while (position < 4 && (b = data.read()) != -1) {
                    position++;
                    pending.write(b);
                    switch (position) {
                        case 2:
                            byte[] bytes = pending.toByteArray();
                            id = (bytes[1] << 8) + bytes[0];
                            pending.reset();
                            break;
                        case 4:
                            bytes = pending.toByteArray();
                            version = (bytes[1] << 8) + bytes[0];
                            pending.reset();
                            break;
                    }
                }

                if (id < 0 || version < 0) {
                    try {
                        subdata.removeClient(Client.this);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    InputStream forward = new InputStream() {
                        boolean open = true;
                        boolean finished = false;

                        @Override
                        public int read() throws IOException {
                            if (open) {
                                int b = data.read();
                                switch (b) {
                                    case '\u0010': // [DLE] (Escape character)
                                        b = data.read();
                                        break;
                                    case '\u0017': // [ETB] (End of Packet character)
                                        finished = true;
                                        b = -1;
                                        break;
                                }
                                if (b <= -1) close();
                                return b;
                            } else return -1;
                        }

                        @Override
                        public void close() throws IOException {
                            if (open) {
                                open = false;
                                if (!socket.isClosed()) {
                                    if (!finished) {
                                        int b;
                                        while ((b = data.read()) != -1 && b != '\u0017') { }
                                    }
                                    Client.this.read();
                                }
                            }
                        }
                    };
                    try {
                        if (!pIn.keySet().contains(id)) throw new IllegalPacketException(getAddress().toString() + ": Could not find handler for packet: [" + id + ", " + version + "]");
                        PacketIn packet = pIn.get(id);
                        if (!packet.isCompatable(version)) throw new IllegalPacketException(getAddress().toString() + ": The handler does not support this packet version (" + packet.version() + "): [" + id + ", " + version + "]");

                        if (packet instanceof PacketStreamIn) {
                            try {
                                ((PacketStreamIn) packet).receive(this, forward);
                            } catch (Throwable e) {
                                throw new InvocationTargetException(e, getAddress().toString() + ": Exception while running packet handler");
                            }
                        } else forward.close();
                    } catch (Throwable e) {
                        SubDataServer.log.error.println(e);
                    }
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException || e instanceof MessageInsufficientBufferException)) SubDataServer.log.error.println(e);
                try {
                    subdata.removeClient(Client.this);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }, "SubDataServer::Packet_Listener(" + address.toString() + ')').start();
    }

    private void write() {
        new Thread(() -> {
            if (queue.size() > 0) try {
                PacketOut next = Util.getDespiteException(() -> queue.get(0), null);
                Util.isException(() -> queue.remove(0));
                if (next != null) {
                    if (!pOut.keySet().contains(next.getClass())) throw new IllegalMessageException(getAddress().toString() + ": Could not find ID for packet: " + next.getClass().getCanonicalName());
                    if (next.version() > 65535 || next.version() < 0) throw new IllegalMessageException(getAddress().toString() + ": Packet version is not in range (0-65535): " + next.getClass().getCanonicalName());

                    out.write(pOut.get(next.getClass()) & 0xFF);
                    out.write((pOut.get(next.getClass()) >>> 8) & 0xFF);
                    out.write(next.version() & 0xFF);
                    out.write((next.version() >>> 8) & 0xFF);
                    out.flush();

                    OutputStream forward = new OutputStream() {
                        boolean open = true;

                        @Override
                        public void write(int b) throws IOException {
                            if (open) {
                                switch (b) {
                                    case '\u0010': // [DLE] (Escape character)
                                    case '\u0017': // [ETB] (End of Packet character)
                                        out.write('\u0010');
                                        break;
                                }
                                out.write(b);
                                out.flush();
                            }
                        }

                        @Override
                        public void close() throws IOException {
                            if (open) {
                                open = false;
                                out.write('\u0017');
                                out.flush();
                                if (queue.size() > 0) Client.this.write();
                                else queue = null;
                            }
                        }
                    };

                    if (next instanceof PacketStreamOut) {
                        ((PacketStreamOut) next).send(forward);
                    } else forward.close();
                }
            } catch (Throwable e) {
                Util.isException(() -> queue.remove(0));
                SubDataServer.log.error.println(e);

                if (queue.size() > 0) Client.this.write();
                else queue = null;
            }
        }, "SubDataServer::Packet_Spooler(" + address.toString() + ')').start();
    }

    /**
     * Send Packet to Client
     *
     * @param packet Packet to send
     */
    public void sendPacket(PacketOut packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (!isClosed()) {
            boolean init = false;

            if (queue == null) {
                queue = new LinkedList<>();
                init = true;
            }
            queue.add(packet);

            if (init) write();
        }
    }

    /**
     * Send Message to Client
     *
     * @param message Packet to send
     */
    public void sendMessage(MessageOut message) {
        if (Util.isNull(message)) throw new NullPointerException();
        sendPacket(new PacketSendMessage(message));
    }

    /**
     * Authorize Connection
     */
    public void authorize() {
        if (ready != null) {
            ready.cancel();
            SubDataServer.log.info.println(socket.getRemoteSocketAddress().toString() + " logged in");
        }
        ready = null;
    }

    /**
     * Get Raw Connection
     *
     * @return Socket
     */
    public Socket getConnection() {
        return socket;
    }

    /**
     * Get if the connection has been closed
     *
     * @return Closed Status
     */
    public boolean isClosed() {
        return closed && socket.isClosed();
    }

    /**
     * Get Remote Address
     *
     * @return Address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * If the connection is ready
     *
     * @return Connection Status
     */
    public boolean isReady() {
        return ready == null;
    }

    /**
     * Gets the Linked Handler
     *
     * @return Handler
     */
    public ClientHandler getHandler() {
        return handler;
    }

    /**
     * Sets the Handler
     *
     * @param obj Handler
     */
    public void setHandler(ClientHandler obj) {
        if (handler != null && handler.getSubData() != null && equals(handler.getSubData())) handler.setSubData(null);
        handler = obj;
        if (handler != null && (handler.getSubData() == null || !equals(handler.getSubData()))) handler.setSubData(this);
    }

    /**
     * Disconnects the Client
     *
     * @throws IOException
     */
    public void disconnect() throws IOException {
        if (!socket.isClosed()) getConnection().close();
        if (handler != null) {
            setHandler(null);
            handler = null;
        }
        closed = true;
        if (subdata.getClients().contains(this)) subdata.removeClient(this);
    }
}
