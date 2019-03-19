package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Galaxi;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Event.SubNetworkConnectEvent;
import net.ME1312.SubData.Server.Protocol.Internal.PacketSendMessage;
import net.ME1312.SubData.Server.Protocol.MessageIn;
import net.ME1312.SubData.Server.Protocol.MessageOut;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DataServer {
    static final HashMap<Class<? extends MessageOut>, NamedContainer<String, String>> mOut = new HashMap<Class<? extends MessageOut>, NamedContainer<String, String>>();
    static final HashMap<String, HashMap<String, MessageIn>> mIn = new HashMap<String, HashMap<String, MessageIn>>();
    static List<String> allowedAddresses = new ArrayList<String>();

    /**
     * Gets the Server Socket
     *
     * @return Server Socket
     */
    public abstract ServerSocket getServer();

    /**
     * Grabs a Client from the Network
     *
     * @param socket Socket to search
     * @return Client
     */
    public abstract Client getClient(Socket socket);

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public abstract Client getClient(InetSocketAddress address);

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public abstract Client getClient(String address);

    /**
     * Grabs all the Clients on the Network
     *
     * @return Client List
     */
    public abstract Collection<Client> getClients();

    /**
     * Register MessageIn to the Network
     *
     * @param channel Message Channel
     * @param handle Handle to Bind
     * @param message MessageIn to register
     */
    public static void registerMessage(String channel, String handle, MessageIn message) {
        if (Util.isNull(message, channel, handle)) throw new NullPointerException();
        HashMap<String, MessageIn> map = (mIn.keySet().contains(channel.toLowerCase()))? mIn.get(channel.toLowerCase()):new HashMap<String, MessageIn>();
        map.put(handle, message);
        mIn.put(channel.toLowerCase(), map);
    }

    /**
     * Unregister MessageIn from the Network
     *
     * @param channel Message Channel
     * @param message MessageIn to unregister
     */
    public static void unregisterMessage(String channel, MessageIn message) {
        if (Util.isNull(channel, message)) throw new NullPointerException();
        if (mIn.keySet().contains(channel.toLowerCase())) {
            List<String> search = new ArrayList<String>();
            search.addAll(mIn.get(channel.toLowerCase()).keySet());
            for (String handle : search) if (mIn.get(channel.toLowerCase()).get(handle).equals(message)) {
                mIn.get(channel.toLowerCase()).remove(handle);
                if (mIn.get(channel.toLowerCase()).size() <= 0)
                    mIn.remove(channel.toLowerCase());
            }
        }
    }

    /**
     * Register MessageOut to the Network
     *
     * @param channel Message Channel
     * @param handle Handle to bind
     * @param message MessageOut to register
     */
    public static void registerMessage(String channel, String handle, Class<? extends MessageOut> message) {
        if (Util.isNull(message, channel, handle)) throw new NullPointerException();
        mOut.put(message, new NamedContainer<String, String>(channel.toLowerCase(), handle));
    }

    /**
     * Unregister MessageOut to the Network
     *
     * @param channel Message Channel
     * @param message MessageOut to unregister
     */
    public static void unregisterMessage(String channel, Class<? extends MessageOut> message) {
        if (Util.isNull(channel, message)) throw new NullPointerException();
        if (mOut.keySet().contains(message) && mOut.get(message).name().equalsIgnoreCase(channel)) mOut.remove(message);
    }

    /**
     * Grab MessageIn Instance via handle
     *
     * @param channel Message Channel
     * @param handle Handle
     * @return MessageIn
     */
    public static MessageIn getMessage(String channel, String handle) {
        if (Util.isNull(channel, handle)) throw new NullPointerException();
        return mIn.get(channel.toLowerCase()).get(handle);
    }

    /**
     * Broadcast a Message to everything on the Network<br>
     * <b>Warning:</b> There are usually different types of applications on the network at once, they may not recognise the same message handles
     *
     * @param message Message to send
     */
    public void broadcastMessage(MessageOut message) {
        if (Util.isNull(message)) throw new NullPointerException();
        List<Client> clients = new ArrayList<Client>();
        clients.addAll(getClients());
        for (Client client : clients) {
            client.sendPacket(new PacketSendMessage(message));
        }
    }

    /**
     * Allow Connections from an Address
     *
     * @param address Address to allow
     */
    public static void allowConnection(String address) {
        if (Util.isNull(address)) throw new NullPointerException();
        if (!allowedAddresses.contains(address)) allowedAddresses.add(address);
    }

    boolean checkConnection(InetAddress address) {
        boolean whitelisted = false;
        Matcher regaddress = Pattern.compile("^(\\d{1,3}).(\\d{1,3}).(\\d{1,3}).(\\d{1,3})$").matcher(address.getHostAddress());
        if (regaddress.find()) {
            for (String allowed : allowedAddresses) if (!whitelisted) {
                Matcher regallowed = Pattern.compile("^(\\d{1,3}|%).(\\d{1,3}|%).(\\d{1,3}|%).(\\d{1,3}|%)$").matcher(allowed);
                if (regallowed.find() && (
                        (regaddress.group(1).equals(regallowed.group(1)) || regallowed.group(1).equals("%")) &&
                                (regaddress.group(2).equals(regallowed.group(2)) || regallowed.group(2).equals("%")) &&
                                (regaddress.group(3).equals(regallowed.group(3)) || regallowed.group(3).equals("%")) &&
                                (regaddress.group(4).equals(regallowed.group(4)) || regallowed.group(4).equals("%"))
                )) whitelisted = true;
            }
        }
        SubNetworkConnectEvent event = new SubNetworkConnectEvent(this, address);
        event.setCancelled(!whitelisted);
        Galaxi.getInstance().getPluginManager().executeEvent(event);
        return !event.isCancelled();
    }

    /**
     * Deny Connections from an Address
     *
     * @param address Address to deny
     */
    public static void denyConnection(String address) {
        if (Util.isNull(address)) throw new NullPointerException();
        allowedAddresses.remove(address);
    }
}
