package net.ME1312.SubData.Client.Library;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.SubData.Client.Protocol.MessageOut;
import net.ME1312.SubData.Client.Protocol.PacketOut;
import net.ME1312.SubData.Client.SubDataClient;
import net.ME1312.SubData.Client.SubDataProtocol;
import net.ME1312.SubData.Client.SubDataSender;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * Forwarded Data Sender Class
 */
public class ForwardedDataSender implements SubDataSender {
    private final SubDataClient client;
    private final UUID id;

    /**
     * Create a Forwarded DataSender
     *
     * @param client SubDataClient
     * @param id Forward ID
     */
    public ForwardedDataSender(SubDataClient client, UUID id) {
        this.client = client;
        this.id = id;
    }

    @Override
    public void ping(Callback<PingResponse> response) {
        client.ping(id, response);
    }

    @Override
    public void sendPacket(PacketOut... packets) {
        try {
            SubDataClient.class.getMethod("forwardPacket", UUID.class, PacketOut[].class).invoke(client, id, packets);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
        } catch (Throwable e) {}
    }

    @Override
    public void sendMessage(MessageOut... messages) {
        try {
            SubDataClient.class.getMethod("forwardMessage", UUID.class, MessageOut[].class).invoke(client, id, messages);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
        } catch (Throwable e) {}
    }

    @Override
    public SubDataClient getConnection() {
        return client;
    }

    @Override
    public SubDataProtocol getProtocol() {
        return client.getProtocol();
    }

    @Override
    public UUID getID() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ForwardedDataSender) {
            return id.equals(((ForwardedDataSender) obj).id);
        } else return super.equals(obj);
    }
}
