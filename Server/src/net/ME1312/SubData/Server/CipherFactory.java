package net.ME1312.SubData.Server;

import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;

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
    NamedContainer<Cipher, String> getCipher(String handle);

    /**
     * Add a cipher
     *
     * @param handle Cipher name
     * @param generator Cipher instance/token generator
     */
    void addCipher(String handle, Util.ReturnRunnable<NamedContainer<Cipher, String>> generator);

    /**
     * Remove a cipher
     *
     * @param handle Cipher name
     */
    void removeCipher(String handle);
}
