package net.ME1312.SubData.Client;
import net.ME1312.SubData.Client.Protocol.PacketOut;

/**
 * SubData Data Sender Layout Class
 */
public interface SubDataSender extends DataSender {

    /**
     * Send a packet to the Sender
     *
     * @param packets Packets to send
     */
    void sendPacket(PacketOut... packets);

    /**
     * Get the Client that connects this Sender to us
     *
     * @return Client
     */
    SubDataClient getConnection();

    /**
     * Get the Protocol used by this Sender
     *
     * @return Sender Protocol
     */
    SubDataProtocol getProtocol();
}
