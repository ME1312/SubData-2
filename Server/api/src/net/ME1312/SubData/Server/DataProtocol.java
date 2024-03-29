package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubData.Server.Protocol.MessageIn;
import net.ME1312.SubData.Server.Protocol.MessageOut;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * SubData Protocol API Class
 */
public abstract class DataProtocol {
    final HashMap<Class<? extends MessageOut>, Pair<String, String>> mOut = new HashMap<Class<? extends MessageOut>, Pair<String, String>>();
    final HashMap<String, HashMap<String, MessageIn>> mIn = new HashMap<String, HashMap<String, MessageIn>>();
    List<String> whitelist = new ArrayList<String>();

    /**
     * Get the Network Protocol Name
     *
     * @return Protocol Name
     */
    public abstract String getName();

    /**
     * Get the Network Protocol Version
     *
     * @return Protocol Version
     */
    public abstract Version getVersion();

    /**
     * Register MessageIn to the Network
     *
     * @param channel Message Channel
     * @param handle Handle to Bind
     * @param message MessageIn to register
     */
    public void registerMessage(String channel, String handle, MessageIn message) {
        Util.nullpo(message, channel, handle);
        HashMap<String, MessageIn> map = (mIn.containsKey(channel.toLowerCase()))? mIn.get(channel.toLowerCase()):new HashMap<String, MessageIn>();
        map.put(handle, message);
        mIn.put(channel.toLowerCase(), map);
    }

    /**
     * Unregister MessageIn from the Network
     *
     * @param channel Message Channel
     * @param message MessageIn to unregister
     */
    public void unregisterMessage(String channel, MessageIn message) {
        Util.nullpo(channel, message);
        if (mIn.containsKey(channel.toLowerCase())) {
            List<String> search = new ArrayList<>(mIn.get(channel.toLowerCase()).keySet());
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
    public void registerMessage(String channel, String handle, Class<? extends MessageOut> message) {
        Util.nullpo(message, channel, handle);
        mOut.put(message, new ContainedPair<String, String>(channel.toLowerCase(), handle));
    }

    /**
     * Unregister MessageOut to the Network
     *
     * @param channel Message Channel
     * @param message MessageOut to unregister
     */
    public void unregisterMessage(String channel, Class<? extends MessageOut> message) {
        Util.nullpo(channel, message);
        if (mOut.containsKey(message) && mOut.get(message).key().equalsIgnoreCase(channel)) mOut.remove(message);
    }

    /**
     * Grab MessageIn Instance via handle
     *
     * @param channel Message Channel
     * @param handle Handle
     * @return MessageIn
     */
    public MessageIn getMessage(String channel, String handle) {
        Util.nullpo(channel, handle);
        return mIn.get(channel.toLowerCase()).get(handle);
    }

    /**
     * Allow Access from an Address (Global)
     *
     * @param address Address to allow
     */
    public void whitelist(String address) {
        Util.nullpo(address);
        if (!whitelist.contains(address)) whitelist.add(address);
    }

    /**
     * Revoke Access from an Address (Global)
     *
     * @param address Address to deny
     */
    public void unwhitelist(String address) {
        Util.nullpo(address);
        whitelist.remove(address);
    }
}
