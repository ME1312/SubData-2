package net.ME1312.SubData.Server.Encryption;

import net.ME1312.SubData.Server.Cipher;
import net.ME1312.SubData.Server.DataClient;

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
    public void encrypt(DataClient client, InputStream in, OutputStream out) throws Exception {
        int i;
        byte[] b = new byte[4096];
        while ((i = in.read(b)) != -1) {
            out.write(b, 0, i);
        }
    }

    @Override
    public void decrypt(DataClient client, InputStream in, OutputStream out) throws Exception {
        int i;
        byte[] b = new byte[4096];
        while ((i = in.read(b)) != -1) {
            out.write(b, 0, i);
        }
    }
}
