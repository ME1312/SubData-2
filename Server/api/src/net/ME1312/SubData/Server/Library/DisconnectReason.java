package net.ME1312.SubData.Server.Library;

/**
 * Disconnect Reason Enum
 */
public enum DisconnectReason {
    /**
     * SubData could not self-initialize within the allotted time
     */
    INITIALIZATION_TIMEOUT,

    /**
     * SubData disconnected because of a Protocol Mismatch
     */
    PROTOCOL_MISMATCH,

    /**
     * SubData disconnected because:<br/>
     * <ul>
     *     <li>The client could not meet the encryption standards</li>
     *     <li>The client was using the right encryption, but the wrong key to encrypt with</li>
     *     <li>The client began sending unintelligible data after the encryption request</li>
     * </ul>
     */
    ENCRYPTION_MISMATCH,

    /**
     * SubData disconnected because the AuthService determined the client's login credentials to be invalid
     * @see net.ME1312.SubData.Server.AuthService
     */
    INVALID_LOGIN,

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
