package net.ME1312.SubData.Client;

import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Encryption.NEH;
import net.ME1312.SubData.Client.Library.Exception.IllegalMessageException;
import net.ME1312.SubData.Client.Library.Exception.IllegalPacketException;
import net.ME1312.SubData.Client.Protocol.*;
import net.ME1312.SubData.Client.Protocol.Internal.PacketSendMessage;
import org.msgpack.core.MessageInsufficientBufferException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class SubDataClient extends DataClient {
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
    private OutputStream out;
    private Socket socket;
    private LinkedList<PacketOut> queue;
    private Runnable shutdown;

    static {
        staticFactory.addCipher("NULL", () -> new NamedContainer<>(NEH.get(), null));
        staticFactory.addCipher("NONE", () -> new NamedContainer<>(NEH.get(), null));
    }

    /**
     * SubData Client Instance
     *
     * @param address Address
     * @param port Port
     * @throws IOException
     */
    public SubDataClient(InetAddress address, int port) throws IOException {
        this(address, port, null);
    }

    /**
     * SubData Client Instance
     *
     * @param address Address
     * @param port Port
     * @param shutdown Shutdown Event
     * @throws IOException
     */
    public SubDataClient(InetAddress address, int port, Runnable shutdown) throws IOException {
        if (Util.isNull(address, port)) throw new NullPointerException();
        this.socket = new Socket(address, port);
        this.out = socket.getOutputStream();
        this.shutdown = shutdown;
        this.queue = null;

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
                        destroy(15);
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
                                    SubDataClient.this.read();
                                }
                            }
                        }
                    };
                    try {
                        if (!pIn.keySet().contains(id)) throw new IllegalPacketException("Could not find handler for packet: [" + id + ", " + version + "]");
                        PacketIn packet = pIn.get(id);
                        if (!packet.isCompatable(version)) throw new IllegalPacketException("The handler does not support this packet version (" + packet.version() + "): [" + id + ", " + version + "]");

                        if (packet instanceof PacketStreamIn) {
                            try {
                                ((PacketStreamIn) packet).receive(forward);
                            } catch (Throwable e) {
                                throw new InvocationTargetException(e, "Exception while running packet handler");
                            }
                        } else forward.close();
                    } catch (Throwable e) {
                        SubDataClient.log.error.println(e);
                    }
                }
            } catch (Exception e) {
                if (!(e instanceof SocketException || e instanceof MessageInsufficientBufferException)) SubDataClient.log.error.println(e);
                try {
                    destroy(15);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }, "SubDataServer::Packet_Listener").start();
    }

    private void write() {
        System.out.println("1");
        new Thread(() -> {
            System.out.println("2");
            if (queue.size() > 0) try {
                PacketOut next = Util.getDespiteException(() -> queue.get(0), null);
                Util.isException(() -> queue.remove(0));
                System.out.println("3");
                if (next != null) {
                    System.out.println("4");
                    if (!pOut.keySet().contains(next.getClass())) throw new IllegalMessageException("Could not find ID for packet: " + next.getClass().getCanonicalName());
                    if (next.version() > 65535 || next.version() < 0) throw new IllegalMessageException("Packet version is not in range (0-65535): " + next.getClass().getCanonicalName());

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
                                System.out.println("6");
                                open = false;
                                out.write('\u0017');
                                out.flush();
                                if (queue.size() > 0) SubDataClient.this.write();
                                else queue = null;
                            }
                        }
                    };

                    if (next instanceof PacketStreamOut) {
                        ((PacketStreamOut) next).send(forward);
                    }
                    else forward.close();
                }
            } catch (Throwable e) {
                Util.isException(() -> queue.remove(0));
                SubDataClient.log.error.println(e);

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

    public Socket getClient() {
        return socket;
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
    public void destroy(int reconnect) throws IOException {
        if (Util.isNull(reconnect)) throw new NullPointerException();
        if (!socket.isClosed()) {
            socket.close();
            log.info.println("The SubData Connection was closed");
        }
    }
}
