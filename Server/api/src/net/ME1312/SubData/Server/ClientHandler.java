package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Map.ObjectMap;

/**
 * Client Handler Layout Class
 */
public interface ClientHandler extends SubDataSerializable {

    /**
     * Gets the SubData Client Channels
     *
     * @return SubData Client Channel Array
     */
    DataClient[] getSubData();

    /**
     * Unlink a SubData Client from this Object
     *
     * @param client Client to Unlink
     */
    void removeSubData(DataClient client);

    @Override
    default ObjectMap<String> forSubData() {
        return null;
    }
}
