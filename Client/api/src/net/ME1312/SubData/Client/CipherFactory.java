package net.ME1312.SubData.Client;

import net.ME1312.Galaxi.Library.NamedContainer;

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
    NamedContainer<Cipher, String> newCipher(String handle);

    /**
     * Get a cipher by name
     *
     * @param handle Cipher name
     * @param key Token
     * @return Cipher
     */
    Cipher getCipher(String handle, String key);
}
