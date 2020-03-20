package net.ME1312.SubData.Client;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Client.Encryption.NEH;
import net.ME1312.SubData.Client.Library.DataSize;
import net.ME1312.SubData.Client.Library.DebugUtil;
import net.ME1312.SubData.Client.Protocol.Initial.InitPacketVerifyState;
import net.ME1312.SubData.Client.Protocol.Internal.*;
import net.ME1312.SubData.Client.Protocol.Internal.PacketDownloadClientList;
import net.ME1312.SubData.Client.Protocol.PacketIn;
import net.ME1312.SubData.Client.Protocol.PacketOut;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * SubData Protocol Class
 */
public class SubDataProtocol extends DataProtocol {
    public static final int MIN_PACKET_ID = 0x0000;
    public static final int MAX_PACKET_ID = 0xFFEF;

    final HashMap<String, Cipher> ciphers = new HashMap<String, Cipher>();
    final HashMap<Class<? extends PacketOut>, Integer> pOut = new HashMap<Class<? extends PacketOut>, Integer>();
    final HashMap<Integer, PacketIn> pIn = new HashMap<Integer, PacketIn>();
    ArrayList<Version> version = new ArrayList<Version>();
    String name;
    Container<Long> bs = new Container<>((long) DataSize.MB);

    /**
     * Create a new Protocol
     */
    public SubDataProtocol() {
        ciphers.put("NULL", NEH.get());
        ciphers.put("NONE", NEH.get());

        pIn.put(0xFFF7, new PacketOpenChannel(null, false));
        pIn.put(0xFFF8, new PacketPingResponse());
        pIn.put(0xFFF9, new PacketPing());
        pIn.put(0xFFFA, new InitPacketVerifyState());
        pIn.put(0xFFFB, new PacketDownloadClientList());
        pIn.put(0xFFFC, new PacketForwardPacket(null, null));
        pIn.put(0xFFFD, new PacketRecieveMessage());
        pIn.put(0xFFFE, new PacketDisconnectUnderstood());
        pIn.put(0xFFFF, new PacketDisconnect());

        pOut.put(PacketOpenChannel.class, 0xFFF7);
        pOut.put(PacketPingResponse.class, 0xFFF8);
        pOut.put(PacketPing.class, 0xFFF9);
        pOut.put(InitPacketVerifyState.class, 0xFFFA);
        pOut.put(PacketDownloadClientList.class, 0xFFFB);
        pOut.put(PacketForwardPacket.class, 0xFFFC);
        pOut.put(PacketSendMessage.class, 0xFFFD);
        pOut.put(PacketDisconnectUnderstood.class, 0xFFFE);
        pOut.put(PacketDisconnect.class, 0xFFFF);
    }

    /**
     * Override this to preform different setup for Client SubChannels
     *
     * @param scheduler Event Scheduler
     * @param logger Network Logger
     * @param address Bind Address (or null for all)
     * @param port Port Number
     * @param login Login Data
     * @see SubDataClient#openChannel()
     * @throws IOException
     */
    protected SubDataClient sub(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port, ObjectMap<?> login) throws IOException {
        return open(scheduler, logger, address, port, login);
    }

    /**
     * Launch a SubData Client Instance
     *
     * @param scheduler Event Scheduler
     * @param logger Network Logger
     * @param address Bind Address (or null for all)
     * @param port Port Number
     * @param login Login Data
     * @throws IOException
     */
    public SubDataClient open(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port, ObjectMap<?> login) throws IOException {
        return new SubDataClient(this, scheduler, logger, address, port, login);
    }

    /**
     * Launch a SubData Client Instance
     *
     * @param scheduler Event Scheduler
     * @param logger Network Logger
     * @param address Bind Address (or null for all)
     * @param port Port Number
     * @throws IOException
     */
    public SubDataClient open(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port) throws IOException {
        return open(scheduler, logger, address, port, null);
    }

    /**
     * Launch a SubData Client Instance
     *
     * @param logger Network Logger
     * @param address Bind Address (or null for all)
     * @param port Port Number
     * @param login Login Data
     * @throws IOException
     */
    public SubDataClient open(Logger logger, InetAddress address, int port, ObjectMap<?> login) throws IOException {
        return open(Runnable::run, logger, address, port, login);
    }

    /**
     * Launch a SubData Client Instance
     *
     * @param logger Network Logger
     * @param address Bind Address (or null for all)
     * @param port Port Number
     * @throws IOException
     */
    public SubDataClient open(Logger logger, InetAddress address, int port) throws IOException {
        return open(Runnable::run, logger, address, port);
    }

    /**
     * Set the Network Protocol Name (may only be called once)
     *
     * @param name Protocol Name
     */
    public void setName(String name) {
        if (this.name != null) throw new IllegalStateException("Protocol name already set");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Add a Protocol Version to the Supported Versions List
     *
     * @param version Protocol Version
     */
    public void addVersion(Version version) {
        if (!this.version.contains(version)) this.version.add(version);
    }

    /**
     * Remove a Protocol Version from the Supported Versions List
     *
     * @param version Protocol Version
     */
    public void removeVersion(Version version) {
        this.version.remove(version);
    }

    public Version[] getVersion() {
        if (version.size() <= 0) {
            return new Version[]{
                    new Version(0)
            };
        } else {
            return version.toArray(new Version[0]);
        }
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
     * @param size Block Size
     */
    public void setBlockSize(long size) {
        bs.set(size);
    }

    /**
     * Register a Cipher to SubData
     *
     * @param cipher Cipher to Add
     * @param handle Handle to Bind
     */
    public void registerCipher(String handle, Cipher cipher) {
        if (Util.isNull(cipher)) throw new NullPointerException();
        if (!handle.equalsIgnoreCase("NULL")) ciphers.put(handle.toUpperCase(), cipher);
    }

    /**
     * Unregister a Cipher from SubData
     *
     * @param handle Handle
     */
    public void unregisterCipher(String handle) {
        if (!handle.equalsIgnoreCase("NULL")) ciphers.remove(handle.toUpperCase());
    }

    /**
     * Register PacketIn to the Network
     *
     * @param id Packet ID (as an unsigned 16-bit value)
     * @param packet PacketIn to register
     */
    public void registerPacket(int id, PacketIn packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (id > MAX_PACKET_ID || id < MIN_PACKET_ID) throw new IllegalArgumentException("Packet ID is not in range (" + DebugUtil.toHex(0xFFFF, MIN_PACKET_ID) + " to " + DebugUtil.toHex(0xFFFF, MAX_PACKET_ID) + "): " + DebugUtil.toHex(0xFFFF, id));
        pIn.put(id, packet);
    }

    /**
     * Unregister PacketIn from the Network
     *
     * @param packet PacketIn to unregister
     */
    public void unregisterPacket(PacketIn packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        List<Integer> search = new ArrayList<Integer>();
        search.addAll(pIn.keySet());
        for (int id : search) if (pIn.get(id).equals(packet) &&  id < MAX_PACKET_ID) {
            pIn.remove(id);
        }
    }

    /**
     * Register PacketOut to the Network
     *
     * @param id Packet ID (as an unsigned 16-bit value)
     * @param packet PacketOut to register
     */
    public void registerPacket(int id, Class<? extends PacketOut> packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (id > MAX_PACKET_ID || id < MIN_PACKET_ID) throw new IllegalArgumentException("Packet ID is not in range (" + DebugUtil.toHex(0xFFFF, MIN_PACKET_ID) + " to " + DebugUtil.toHex(0xFFFF, MAX_PACKET_ID) + "): " + DebugUtil.toHex(0xFFFF, id));
        pOut.put(packet, id);
    }

    /**
     * Unregister PacketOut to the Network
     *
     * @param packet PacketOut to unregister
     */
    public void unregisterPacket(Class<? extends PacketOut> packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (pOut.keySet().contains(packet) && pOut.get(packet) < MAX_PACKET_ID) pOut.remove(packet);
    }

    /**
     * Grab PacketIn Instance via ID
     *
     * @param id Packet ID (as an unsigned 16-bit value)
     * @return PacketIn
     */
    public PacketIn getPacket(int id) {
        return pIn.get(id);
    }
}
