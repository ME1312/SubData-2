package net.ME1312.SubData.Server.Library.Exception;

/**
 * Illegal Packet Exception
 */
public class IllegalPacketException extends IllegalStateException {
    public IllegalPacketException() {}
    public IllegalPacketException(String s) {
        super(s);
    }
}
