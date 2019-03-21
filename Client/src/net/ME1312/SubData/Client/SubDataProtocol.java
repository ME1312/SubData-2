package net.ME1312.SubData.Client;

import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Encryption.NEH;
import net.ME1312.SubData.Client.Protocol.Internal.PacketRecieveMessage;
import net.ME1312.SubData.Client.Protocol.Internal.PacketSendMessage;
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
    final HashMap<String, Cipher> ciphers = new HashMap<String, Cipher>();
    final HashMap<Class<? extends PacketOut>, Integer> pOut = new HashMap<Class<? extends PacketOut>, Integer>();
    final HashMap<Integer, PacketIn> pIn = new HashMap<Integer, PacketIn>();
    Logger log;

    /**
     * Create a new Protocol
     *
     * @param logger SubData Log Channel
     */
    public SubDataProtocol(Logger logger) {
        log = logger;

        ciphers.put("NULL", NEH.get());
        ciphers.put("NONE", NEH.get());

        pOut.put(PacketSendMessage.class, 0x0000);
        pIn.put(0x0000, new PacketRecieveMessage());
    }

    /**
     * Launch a SubData Client Instance
     *
     * @param address Bind Address (or null for all)
     * @param port Port Number
     * @throws IOException
     */
    public SubDataClient open(InetAddress address, int port) throws IOException {
        return open(address, port, null);
    }

    /**
     * Launch a SubData Client Instance
     *
     * @param address Bind Address (or null for all)
     * @param port Port Number
     * @param shutdown Shutdown Event
     * @throws IOException
     */
    public SubDataClient open(InetAddress address, int port, Runnable shutdown) throws IOException {
        return new SubDataClient(this, address, port, shutdown);
    }

    /**
     * Add a Cipher to SubData
     *
     * @param cipher Cipher to Add
     * @param handle Handle to Bind
     */
    public void addCipher(String handle, Cipher cipher) {
        if (Util.isNull(cipher)) throw new NullPointerException();
        if (!handle.equalsIgnoreCase("NULL")) ciphers.put(handle.toUpperCase(), cipher);
    }

    /**
     * Remove a Cipher from SubData
     *
     * @param handle Handle
     */
    public void removeCipher(String handle) {
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
        if (id > 65535 || id <= 0) throw new IllegalArgumentException("Packet ID is not in range (1-65535): " + id);
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
        for (int id : search) if (pIn.get(id).equals(packet) && id > 0) {
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
        if (id > 65535 || id <= 0) throw new IllegalArgumentException("Packet ID is not in range (1-65535): " + id);
        pOut.put(packet, id);
    }

    /**
     * Unregister PacketOut to the Network
     *
     * @param packet PacketOut to unregister
     */
    public void unregisterPacket(Class<? extends PacketOut> packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        if (pOut.keySet().contains(packet) && pOut.get(packet) > 0) pOut.remove(packet);
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
