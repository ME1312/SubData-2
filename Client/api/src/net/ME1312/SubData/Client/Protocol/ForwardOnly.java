package net.ME1312.SubData.Client.Protocol;

/**
 * Forward-Only Packet/Message Tag Class<br>
 * Packets/Messages that implement this may only be forwarded to other clients (they cannot be sent to the server)
 */
public interface ForwardOnly extends Forwardable {
}
