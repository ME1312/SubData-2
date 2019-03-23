package net.ME1312.SubData.Server.Encryption;

import net.ME1312.SubData.Server.Cipher;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Null Encryption Handler Class
 */
public final class NEH implements Cipher {
    private static final NEH instance = new NEH();

    /**
     * Get the NEH instance
     *
     * @return NEH
     */
    public static NEH get() {
        return instance;
    }

    private NEH() {}

    @Override
    public String getName() {
        return "NULL";
    }

    @Override
    public void encrypt(InputStream in, OutputStream out) throws Exception {
        int b;
        while((b = in.read()) != -1) out.write(b);
    }

    @Override
    public void decrypt(InputStream in, OutputStream out) throws Exception {
        int b;
        while((b = in.read()) != -1) out.write(b);
    }
}
