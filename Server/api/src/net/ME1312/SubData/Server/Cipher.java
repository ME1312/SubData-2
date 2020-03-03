package net.ME1312.SubData.Server;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * SubData Cipher Layout Class
 */
public interface Cipher {
    /**
     * Get the name of this Cipher
     *
     * @return Cipher Name
     */
    String getName();

    /**
     * Encrypt Data
     *
     * @param client Client
     * @param in Decrypted Data
     * @param out Encrypted Data
     */
    void encrypt(DataClient client, InputStream in, OutputStream out) throws Exception;

    /**
     * Decrypt Data
     *
     * @param client Client
     * @param in Encrypted Data
     * @param out Decrypted Data
     */
    void decrypt(DataClient client, InputStream in, OutputStream out) throws Exception;

    /**
     * Retire this encryption method for a specified Client
     *
     * @param client Client
     */
    default void retire(DataClient client) {}
}
