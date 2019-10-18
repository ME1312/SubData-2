package net.ME1312.SubData.Client.Library.Exception;

/**
 * Illegal Packet Sender Exception
 */
public class IllegalSenderException extends IllegalStateException {
    public IllegalSenderException() {}
    public IllegalSenderException(String s) {
        super(s);
    }
}
