package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Map.ObjectMap;

/**
 * Authentication Service Layout Class
 */
public interface AuthService<K> {

    /**
     * Authenticate a Client
     *
     * @param client Client to Authenticate
     * @param data Login Data
     * @return Authentication Result (or null if invalid)
     */
    Object authenticate(DataClient client, ObjectMap<K> data);
}
