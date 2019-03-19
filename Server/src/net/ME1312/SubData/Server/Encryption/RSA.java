package net.ME1312.SubData.Server.Encryption;

import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.CipherFactory;

import javax.crypto.Cipher;
import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

/**
 * RSA Encryption Handler Class
 */
public final class RSA implements net.ME1312.SubData.Server.Cipher, CipherFactory {

    // Supported Forward Ciphers
    private static final HashMap<String, Util.ReturnRunnable<NamedContainer<net.ME1312.SubData.Server.Cipher, String>>> forward = new HashMap<String, Util.ReturnRunnable<NamedContainer<net.ME1312.SubData.Server.Cipher, String>>>();

    // RSA specification
    private static final String CIPHER_SPEC = "RSA/ECB/PKCS1Padding";

    // Process input/output streams in chunks
    private static final int BUFFER_SIZE = 1024;

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    static {
        forward.put("AES", () -> AES.random(128));
        forward.put("AES-128", () -> AES.random(128));
        forward.put("AES-192", () -> AES.random(192));
        forward.put("AES-256", () -> AES.random(256));
    }

    @Override
    public String getName() {
        return "RSA";
    }

    /**
     * Initialize RSA Cipher (Server)
     *
     * @param keyLength 2048, 3072, 4096 bit mode
     * @param privateKey Private key location (will generate if unavailable)
     * @param publicKey Public key location (will generate if unavailable)
     * @throws Exception
     */
    public RSA(int keyLength, File privateKey, File publicKey) throws Exception {
        if (!privateKey.exists() || !publicKey.exists()) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(keyLength);
            KeyPair kp = kpg.generateKeyPair();

            try (FileOutputStream out = new FileOutputStream(privateKey)) {
                out.write(kp.getPrivate().getEncoded());
            }

            try (FileOutputStream out = new FileOutputStream(publicKey)) {
                out.write(kp.getPublic().getEncoded());
            }
            this.privateKey = kp.getPrivate();
            this.publicKey = kp.getPublic();
        } else {
            byte[] bytes = Files.readAllBytes(privateKey.toPath());
            PKCS8EncodedKeySpec ks1 = new PKCS8EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.privateKey = kf.generatePrivate(ks1);

            bytes = Files.readAllBytes(publicKey.toPath());
            X509EncodedKeySpec ks2 = new X509EncodedKeySpec(bytes);
            this.publicKey = kf.generatePublic(ks2);
        }
    }

    /**
     * Initialize RSA Cipher (Client)
     *
     * @param publicKey Public key location
     * @throws Exception
     */
    public RSA(File publicKey) throws Exception {
        byte[] bytes = Files.readAllBytes(publicKey.toPath());
        X509EncodedKeySpec ks = new X509EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.privateKey = null;
        this.publicKey = kf.generatePublic(ks);
    }

    @Override
    public void encrypt(InputStream in, OutputStream out) throws Exception {
        // initialize RSA encryption
        Cipher ci = Cipher.getInstance(CIPHER_SPEC);
        ci.init(Cipher.ENCRYPT_MODE, (privateKey != null)?privateKey:publicKey);

        // read data from input into buffer, encrypt and write to output
        byte[] ibuf = new byte[BUFFER_SIZE];
        int len;
        while ((len = in.read(ibuf)) != -1) {
            byte[] obuf = ci.update(ibuf, 0, len);
            if (obuf != null) out.write(obuf);
        }
        byte[] obuf = ci.doFinal();
        if (obuf != null) out.write(obuf);
    }

    @Override
    public void decrypt(InputStream in, OutputStream out) throws Exception {
        // initialize RSA encryption
        Cipher ci = Cipher.getInstance(CIPHER_SPEC);
        ci.init(Cipher.DECRYPT_MODE, (privateKey != null)?privateKey:publicKey);

        // read data from input into buffer, decrypt and write to output
        byte[] ibuf = new byte[BUFFER_SIZE];
        int len;
        while ((len = in.read(ibuf)) != -1) {
            byte[] obuf = ci.update(ibuf, 0, len);
            if (obuf != null) out.write(obuf);
        }
        byte[] obuf = ci.doFinal();
        if (obuf != null) out.write(obuf);
    }

    @Override
    public NamedContainer<net.ME1312.SubData.Server.Cipher, String> getCipher(String handle) {
        return forward.getOrDefault(handle.toUpperCase(), () -> new NamedContainer<>(this, null)).run();
    }

    @Override
    public void addCipher(String handle, Util.ReturnRunnable<NamedContainer<net.ME1312.SubData.Server.Cipher, String>> generator) {
        if (Util.isNull(generator)) throw new NullPointerException();
        handle = handle.toUpperCase();
        if (!forward.keySet().contains(handle)) forward.put(handle, generator);
    }

    @Override
    public void removeCipher(String handle) {
        forward.remove(handle.toUpperCase());
    }
}
