package net.ME1312.SubData.Client.Protocol.Internal;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.Library.PingResponse;
import net.ME1312.SubData.Client.Protocol.Forwardable;
import net.ME1312.SubData.Client.Protocol.PacketObjectIn;
import net.ME1312.SubData.Client.Protocol.PacketObjectOut;
import net.ME1312.SubData.Client.SubDataSender;

import java.util.Calendar;

import static net.ME1312.SubData.Client.Protocol.Internal.PacketPing.callbacks;

/**
 * Ping Response Packet
 */
public class PacketPingResponse implements Forwardable, PacketObjectOut<Integer>, PacketObjectIn<Integer> {
    private ObjectMap<Integer> data;
    private long init;

    /**
     * New PacketPingResponse (In)
     */
    public PacketPingResponse() {}

    /**
     * New PacketPingResponse (Out)
     *
     * @param data Previous Data
     */
    public PacketPingResponse(ObjectMap<Integer> data) {
        if (Util.isNull(data)) throw new NullPointerException();
        init = Calendar.getInstance().getTime().getTime();
        this.data = data;
    }

    @Override
    public ObjectMap<Integer> send(SubDataSender sender) throws Throwable {
        data.set(0x0003, init); // Object Initialization (Remote)
        data.set(0x0004, Calendar.getInstance().getTime().getTime()); // On Send (Remote)
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receive(SubDataSender sender, ObjectMap<Integer> data) throws Throwable {
        data.set(0x0005, Calendar.getInstance().getTime().getTime()); // Transaction Complete

        for (Callback<PingResponse> callback : callbacks.get(data.getRawString(0x0000))) callback.run(new PingResponse(data));
        callbacks.remove(data.getRawString(0x0000));
    }

    @Override
    public int version() {
        return 0x0001;
    }
}
