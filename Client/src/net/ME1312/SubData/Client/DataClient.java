package net.ME1312.SubData.Client;

import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Protocol.MessageIn;
import net.ME1312.SubData.Client.Protocol.MessageOut;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class DataClient {
    static final HashMap<Class<? extends MessageOut>, NamedContainer<String, String>> mOut = new HashMap<Class<? extends MessageOut>, NamedContainer<String, String>>();
    static final HashMap<String, HashMap<String, MessageIn>> mIn = new HashMap<String, HashMap<String, MessageIn>>();

    /**
     * Gets the Client Socket
     *
     * @return Client Socket
     */
    public abstract Socket getClient();

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
     * Send Message to Client
     *
     * @param message Packet to send
     */
    public abstract void sendMessage(MessageOut message);
}
