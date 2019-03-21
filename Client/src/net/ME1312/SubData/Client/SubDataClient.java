package net.ME1312.SubData.Client;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Encryption.NEH;
import net.ME1312.SubData.Client.Library.Exception.EndOfStreamException;
import net.ME1312.SubData.Client.Library.Exception.IllegalMessageException;
import net.ME1312.SubData.Client.Library.Exception.IllegalPacketException;
import net.ME1312.SubData.Client.Protocol.*;
import net.ME1312.SubData.Client.Protocol.Internal.PacketSendMessage;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

/**
 * SubData Client Class
 */
public class SubDataClient extends DataClient {
    private Socket socket;
    private OutputStream out;
    private SubDataProtocol protocol;
    private Cipher cipher = NEH.get();
    private LinkedList<PacketOut> queue;
    private Runnable shutdown;

    SubDataClient(SubDataProtocol protocol, InetAddress address, int port, Runnable shutdown) throws IOException {
        if (Util.isNull(address, port)) throw new NullPointerException();
        this.protocol = protocol;
        this.socket = new Socket(address, port);
        this.out = socket.getOutputStream();
        this.shutdown = shutdown;
        this.queue = null;

        protocol.log.info("Connected to " + socket.getRemoteSocketAddress());

        read();
    }

    private void read() {
        if (!socket.isClosed()) new Thread(() -> {
            try {
                InputStream in = socket.getInputStream();
                InputStream raw = new InputStream() {
                    boolean open = true;
                    boolean finished = false;

                    private int next() throws IOException {
                        int b = in.read();
                        switch (b) {
                            case -1:
                                throw new EndOfStreamException();
                            case '\u0010': // [DLE] (Escape character)
                                b = in.read();
                                break;
                            case '\u0017': // [ETB] (End of Packet character)
                                finished = true;
                                b = -1;
                                break;
                        }
                        return b;
                    }

                    @Override
                    public int read() throws IOException {
                        if (open) {
                            int b = next();
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
                                    while (next() != -1);
                                }
                                SubDataClient.this.read();
                            }
                        }
                    }
                };

                PipedInputStream data = new PipedInputStream(1024);
                new Thread(() -> {
                    try {
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

                        if (id >= 0 && version >= 0) {
                            try (InputStream forward = new InputStream() {
                                boolean open = true;

                                @Override
                                public int read() throws IOException {
                                    if (open) {
                                        int b = data.read();
                                        if (b < 0) close();
                                        return b;
                                    } else return -1;
                                }

                                @Override
                                public void close() throws IOException {
                                    open = false;
                                    while (data.read() != -1);
                                }
                            }) {
                                HashMap<Integer, PacketIn> pIn = protocol.pIn;
                                if (!pIn.keySet().contains(id)) throw new IllegalPacketException("Could not find handler for packet: [" + id + ", " + version + "]");
                                PacketIn packet = pIn.get(id);
                                if (!packet.isCompatable(version)) throw new IllegalPacketException("The handler does not support this packet version (" + packet.version() + "): [" + id + ", " + version + "]");

                                if (packet instanceof PacketStreamIn) {
                                    try {
                                        ((PacketStreamIn) packet).receive(this, forward);
                                    } catch (Throwable e) {
                                        throw new InvocationTargetException(e, "Exception while running packet handler");
                                    }
                                } else forward.close();
                            } catch (Throwable e) {
                                StringWriter sw = new StringWriter();
                                e.printStackTrace(new PrintWriter(sw));
                                protocol.log.severe(sw.toString());
                            }
                        }
                    } catch (Exception e) {
                        if (!(e instanceof SocketException && !Boolean.getBoolean("subdata.debug"))) {
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            protocol.log.severe(sw.toString());
                        }
                        try {
                            close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }, "SubDataClient::Packet_Decoder").start();

                PipedOutputStream forward = new PipedOutputStream(data);
                cipher.decrypt(raw, forward);
                forward.close();
            } catch (Exception e) {
                if (!(e instanceof SocketException && !Boolean.getBoolean("subdata.debug"))) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    protocol.log.severe(sw.toString());
                }
                try {
                    close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }, "SubDataClient::Packet_Listener").start();
    }

    private void write() {
        if (!socket.isClosed()) new Thread(() -> {
            if (queue.size() > 0) try {
                PacketOut next = Util.getDespiteException(() -> queue.get(0), null);
                Util.isException(() -> queue.remove(0));
                if (next != null) {
                    PipedOutputStream data = new PipedOutputStream();
                    PipedInputStream raw = new PipedInputStream(data, 1024);
                    new Thread(() -> {
                        try (OutputStream forward = new OutputStream() {
                            boolean open = true;

                            @Override
                            public void write(int b) throws IOException {
                                if (open) data.write(b);
                            }

                            @Override
                            public void close() throws IOException {
                                if (open) open = false;
                            }
                        }) {
                            HashMap<Class<? extends PacketOut>, Integer> pOut = protocol.pOut;
                            if (!pOut.keySet().contains(next.getClass())) throw new IllegalMessageException("Could not find ID for packet: " + next.getClass().getCanonicalName());
                            if (next.version() > 65535 || next.version() < 0) throw new IllegalMessageException("Packet version is not in range (0-65535): " + next.getClass().getCanonicalName());

                            data.write(pOut.get(next.getClass()) & 0xFF);
                            data.write((pOut.get(next.getClass()) >>> 8) & 0xFF);
                            data.write(next.version() & 0xFF);
                            data.write((next.version() >>> 8) & 0xFF);
                            data.flush();

                            if (next instanceof PacketStreamOut) {
                                ((PacketStreamOut) next).send(this, forward);
                            } else forward.close();
                        } catch (Throwable e) {
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            protocol.log.severe(sw.toString());
                        }
                        Util.isException(data::close);
                    }, "SubDataServer::Packet_Encoder").start();

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
                                if (queue.size() > 0) SubDataClient.this.write();
                                else queue = null;
                            }
                        }
                    };
                    cipher.encrypt(raw, forward);
                    forward.close();
                    raw.close();
                }
            } catch (Throwable e) {
                Util.isException(() -> queue.remove(0));

                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                protocol.log.severe(sw.toString());

                if (queue.size() > 0) SubDataClient.this.write();
                else queue = null;
            }
        }, "SubDataServer::Packet_Spooler").start();
    }

    /**
     * Send Packet to Client
     *
     * @param packet Packet to send
     */
    public void sendPacket(PacketOut packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (!socket.isClosed()) {
            boolean init = false;

            if (queue == null) {
                queue = new LinkedList<>();
                init = true;
            }
            queue.add(packet);

            if (init) write();
        }
    }

    public void sendMessage(MessageOut message) {
        if (Util.isNull(message)) throw new NullPointerException();
        sendPacket(new PacketSendMessage(message));
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
     * Get the Protocol for this Server
     *
     * @return Server Protocol
     */
    public SubDataProtocol getProtocol() {
        return protocol;
    }

    public InetSocketAddress getAddress() {
        return new InetSocketAddress(socket.getInetAddress(), socket.getPort());
    }

    public void close() throws IOException {
        if (!socket.isClosed()) {
            socket.close();
            protocol.log.info("Disconnected from " + socket.getRemoteSocketAddress());
        }
    }

    public boolean isClosed() {
        return socket.isClosed();
    }
}
