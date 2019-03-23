package net.ME1312.SubData.Server.Library;

/**
 * Connection State Enum
 */
public enum ConnectionState {
    /**
     * This defines the state before any packets have successfully been received<br>
     * <b>Only InitPacketDeclaration may be received in this state<b>
     */
    PRE_INITIALIZATION(0),

    /**
     * This defines the state where only the initialization protocol is active<br/>
     * <b>App-defined packets cannot be transferred in this state</b>
     */
    INITIALIZATION(1),

    /**
     * This defines the state where the app-defined protocol is active and ready for use
     */
    READY(5),

    /**
     * This signifies that the connection is closing gracefully<br/>
     * <b>Only PacketDisconnectUnderstood may be received in this state<b>
     */
    CLOSING(9),

    /**
     * This means that the connection is closed<br/>
     * <b>No further data may be exchanged in this state</b>
     */
    CLOSED(10);

    int step;
    ConnectionState(int step) {
        this.step = step;
    }

    /**
     * Get this step as an Integer
     *
     * @return Integer value
     */
    public int asInt() {
        return step;
    }}
