package net.ME1312.SubData.Client;

import net.ME1312.Galaxi.Library.Callback.Callback;
import net.ME1312.SubData.Client.Library.PingResponse;
import net.ME1312.SubData.Client.Protocol.MessageOut;

import java.util.UUID;

/**
 * SubData Data Sender API Layout Class
 */
public interface DataSender {

    /**
     * Ping the Sender
     *
     * @param response Ping Response
     */
    void ping(Callback<PingResponse> response);

    /**
     * Send a message to the Sender
     *
     * @param messages Messages to send
     */
    void sendMessage(MessageOut... messages);

    /**
     * Get the Client that connects this Sender to us
     *
     * @return Client
     */
    DataClient getConnection();

    /**
     * Get the Protocol used by this Sender
     *
     * @return Sender Protocol
     */
    DataProtocol getProtocol();

    /**
     * Get the ID of this Sender
     *
     * @return Sender ID
     */
    UUID getID();
}
