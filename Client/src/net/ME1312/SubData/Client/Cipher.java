package net.ME1312.SubData.Client;

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
     * @param in Decrypted Data
     * @param out Encrypted Data
     */
    void encrypt(InputStream in, OutputStream out) throws Exception;

    /**
     * Decrypt Data
     *
     * @param in Encrypted Data
     * @param out Decrypted Data
     */
    void decrypt(InputStream in, OutputStream out) throws Exception;
}
