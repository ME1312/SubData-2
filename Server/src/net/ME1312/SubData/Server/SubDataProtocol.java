package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.Encryption.NEH;
import net.ME1312.SubData.Server.Library.DebugUtil;
import net.ME1312.SubData.Server.Protocol.Internal.*;
import net.ME1312.SubData.Server.Protocol.PacketIn;
import net.ME1312.SubData.Server.Protocol.PacketOut;

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
    final HashMap<String, Cipher> ciphers = new HashMap<String, Cipher>();
    final HashMap<Class<? extends PacketOut>, Integer> pOut = new HashMap<Class<? extends PacketOut>, Integer>();
    final HashMap<Integer, PacketIn> pIn = new HashMap<Integer, PacketIn>();
    int MAX_QUEUE = 64;
    Version version;
    String name;

    /**
     * Create a new Protocol
     */
    public SubDataProtocol() {
        ciphers.put("NULL", NEH.get());
        ciphers.put("NONE", NEH.get());

        pIn.put(0xFFFB, new PacketDownloadClientList(null, null));
        pIn.put(0xFFFC, new PacketForwardPacket(null));
        pIn.put(0xFFFD, new PacketRecieveMessage());
        pIn.put(0xFFFE, new PacketDisconnectUnderstood());
        pIn.put(0xFFFF, new PacketDisconnect());

        pOut.put(PacketDownloadClientList.class, 0xFFFB);
        pOut.put(PacketForwardPacket.class, 0xFFFC);
        pOut.put(PacketSendMessage.class, 0xFFFD);
        pOut.put(PacketDisconnectUnderstood.class, 0xFFFE);
        pOut.put(PacketDisconnect.class, 0xFFFF);
    }

    /**
     * SubData Server Instance
     *
     * @param scheduler Event Scheduler
     * @param logger Network Logger
     * @param address Bind Address (or null for all)
     * @param port Port Number
     * @param cipher Cipher (or null for none)
     * @throws IOException
     */
    public SubDataServer open(Callback<Runnable> scheduler, Logger logger, InetAddress address, int port, String cipher) throws IOException {
        return new SubDataServer(this, scheduler, logger, address, port, cipher);
    }

    /**
     * SubData Server Instance
     *
     * @param logger Network Logger
     * @param address Bind Address (or null for all)
     * @param port Port Number
     * @param cipher Cipher (or null for none)
     * @throws IOException
     */
    public SubDataServer open(Logger logger, InetAddress address, int port, String cipher) throws IOException {
        return open(Runnable::run, logger, address, port, cipher);
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
     * Set the Network Protocol Version (may only be called once)
     *
     * @param version Protocol Version
     */
    public void setVersion(Version version) {
        if (this.version != null) throw new IllegalStateException("Protocol version already set");
        this.version = version;
    }

    public Version getVersion() {
        return (version != null)?version:new Version(0);
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
        if (id > 65530 || id < 0) throw new IllegalArgumentException("Packet ID is not in range (0x0000 to 0xFFFA): " + DebugUtil.toHex(0xFFFF, id));
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
        for (int id : search) if (pIn.get(id).equals(packet) && id < 65530) {
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
        if (id > 65530 || id < 0) throw new IllegalArgumentException("Packet ID is not in range (0x0000 to 0xFFFA): " + DebugUtil.toHex(0xFFFF, id));
        pOut.put(packet, id);
    }

    /**
     * Unregister PacketOut to the Network
     *
     * @param packet PacketOut to unregister
     */
    public void unregisterPacket(Class<? extends PacketOut> packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (pOut.keySet().contains(packet) && pOut.get(packet) < 65530) pOut.remove(packet);
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
