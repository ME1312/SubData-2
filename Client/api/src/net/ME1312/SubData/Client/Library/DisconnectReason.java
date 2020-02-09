package net.ME1312.SubData.Client.Library;

/**
 * Disconnect Reason Enum
 */
public enum DisconnectReason {
    /**
     * SubData disconnected because of a Protocol Mismatch
     */
    PROTOCOL_MISMATCH,

    /**
     * SubData disconnected because:<br/>
     * <ul>
     *     <li>We could not meet the server's encryption standards</li>
     *     <li>We were using the right encryption, but the wrong key to encrypt with</li>
     *     <li>The server began sending unintelligible data after the encryption request</li>
     * </ul>
     */
    ENCRYPTION_MISMATCH,

    /**
     * SubData disconnected because an unhandled exception occurred
     */
    UNHANDLED_EXCEPTION,

    /**
     * SubData disconnected because the socket connection was interrupted
     */
    CONNECTION_INTERRUPTED,

    /**
     * SubData disconnected because it was instructed to do so
     */
    CLOSE_REQUESTED,
}
