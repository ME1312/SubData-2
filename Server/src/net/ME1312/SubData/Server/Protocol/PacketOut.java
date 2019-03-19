package net.ME1312.SubData.Server.Protocol;

/**
 * Packet Out Layout Class
 */
public interface PacketOut {

    /**
     * Protocol Version
     *
     * @return Version (as an unsigned 16-bit value)
     */
    int version();
}
