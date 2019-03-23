package net.ME1312.SubData.Server;

/**
 * Client Handler Layout Class
 */
public interface ClientHandler {
    /**
     * Gets the SubData Client
     *
     * @return SubData Client (or null if not linked)
     */
    DataClient getSubData();

    /**
     * Link a SubData Client to this Object
     *
     * @param client Client to Link
     */
    void setSubData(DataClient client);
}
