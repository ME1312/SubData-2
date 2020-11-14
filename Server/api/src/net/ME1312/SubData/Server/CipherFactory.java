package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.Container.Pair;

/**
 * Cipher Factory Layout Class
 */
public interface CipherFactory {

    /**
     * Get a cipher by name
     *
     * @param handle Cipher name
     * @return Cipher/Token pair
     */
    Pair<Cipher, String> newCipher(String handle);

    /**
     * Get a cipher by name
     *
     * @param handle Cipher name
     * @param key Token
     * @return Cipher
     */
    Cipher getCipher(String handle, String key);
}
