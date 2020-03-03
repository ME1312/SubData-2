package net.ME1312.SubData.Server.Encryption;

import net.ME1312.Galaxi.Library.Callback.ReturnCallback;
import net.ME1312.Galaxi.Library.Callback.ReturnRunnable;
import net.ME1312.Galaxi.Library.Container.Container;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.CipherFactory;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.Library.EscapedOutputStream;
import net.ME1312.SubData.Server.Library.Exception.EncryptionException;

import javax.crypto.Cipher;
import java.io.*;
import java.net.SocketException;
import java.nio.file.Files;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

/**
 * RSA Encryption Handler Class
 */
public final class RSA implements net.ME1312.SubData.Server.Cipher, CipherFactory {

    // Supported Forward Ciphers
    private static final HashMap<String, ReturnRunnable<NamedContainer<net.ME1312.SubData.Server.Cipher, String>>> forwardG = new HashMap<String, ReturnRunnable<NamedContainer<net.ME1312.SubData.Server.Cipher, String>>>();
    private static final HashMap<String, ReturnCallback<String, net.ME1312.SubData.Server.Cipher>> forwardP = new HashMap<String, ReturnCallback<String, net.ME1312.SubData.Server.Cipher>>();

    // RSA specification
    private static final String CIPHER_SPEC = "RSA/ECB/PKCS1Padding";

    // Process input/output streams in chunks
    private final int BUFFER_SIZE;

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    static {
        addCipher("AES", () -> AES.random(128), key -> new AES(128, key));
        addCipher("AES-128", () -> AES.random(128), key -> new AES(128, key));
        addCipher("AES-192", () -> AES.random(192), key -> new AES(192, key));
        addCipher("AES-256", () -> AES.random(256), key -> new AES(256, key));
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
            kf = KeyFactory.getInstance("RSA");
            this.publicKey = kf.generatePublic(ks2);
        }
        BUFFER_SIZE = (keyLength / 8) - 11;
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
        BUFFER_SIZE = (((RSAPublicKey) this.publicKey).getModulus().bitLength() / 8) - 11;
    }

    @Override
    public void encrypt(DataClient client, InputStream in, OutputStream out) throws Exception {
        EscapedOutputStream stream = new EscapedOutputStream(out, '\u0010', '\u000F');
        Container<Boolean> reset = new Container<>(false);
        while (!reset.get()) {
            Container<Boolean> wrote = new Container<>(false);
            encrypt(new InputStream() {
                boolean open = true;
                int bc = 1;

                private int next() throws IOException {
                    if (bc > BUFFER_SIZE) {
                        return -1;
                    } else {
                        int b = in.read();
                        if (b == -1) reset.set(true);
                        else wrote.set(true);
                        return b;
                    }
                }

                @Override
                public int read() throws IOException {
                    if (open) {
                        int b = next();
                        if (b <= -1) close();
                        else ++bc;
                        return b;
                    } else return -1;
                }

                @Override
                public void close() throws IOException {
                    if (open) {
                        open = false;
                    }
                }
            }, stream);
            if (wrote.get()) {
                stream.control('\u000F');
                stream.flush();
            }
        }
    }
    private void encrypt(InputStream in, OutputStream out) throws Exception {
        try {
            // initialize RSA encryption
            Cipher ci = Cipher.getInstance(CIPHER_SPEC);
            ci.init(Cipher.ENCRYPT_MODE, (privateKey != null) ? privateKey : publicKey);

            // read data from input into buffer, encrypt and write to output
            byte[] ibuf = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(ibuf)) != -1) {
                byte[] obuf = ci.update(ibuf, 0, len);
                if (obuf != null) out.write(obuf);
            }
            byte[] obuf = ci.doFinal();
            if (obuf != null) out.write(obuf);
        } catch (SocketException e) {
            throw e;
        } catch (Throwable e) {
            throw new EncryptionException(e, "Could not encrypt data");
        }
    }


    @Override
    public void decrypt(DataClient client, InputStream in, OutputStream out) throws Exception {
        Container<Boolean> reset = new Container<>(false);
        while (!reset.get()) {
            decrypt(new InputStream() {
                boolean open = true;
                Integer pending = null;

                private int next() throws IOException {
                    int b = (pending != null)?pending:in.read();
                    pending = null;

                    switch (b) {
                        case -1:
                            reset.set(true);
                        case '\u0010':
                            int next = in.read();
                            switch (next) {
                                case '\u0010': // [DLE] (Escape character)
                                    /* no action necessary */
                                    break;
                                case '\u000F': //  [SI] (End of Frame character)
                                    b = -1;
                                    break;
                                default:
                                    pending = next;
                                    break;
                            }
                            break;
                    }
                    return b;
                }

                @Override
                public int read() throws IOException {
                    if (open) {
                        int b = next();
                        if (b <= -1) close();
                        return b;
                    } else return -1;
                }

                @Override
                public void close() throws IOException {
                    if (open) {
                        open = false;
                    }
                }
            }, out);
        }
    }
    private void decrypt(InputStream in, OutputStream out) throws Exception {
        try {
            // initialize RSA encryption
            Cipher ci = Cipher.getInstance(CIPHER_SPEC);
            ci.init(Cipher.DECRYPT_MODE, (privateKey != null) ? privateKey : publicKey);

            // read data from input into buffer, decrypt and write to output
            byte[] ibuf = new byte[BUFFER_SIZE];
            boolean wrote = false;
            int len;
            while ((len = in.read(ibuf)) != -1) {
                byte[] obuf = ci.update(ibuf, 0, len);
                if (obuf != null) out.write(obuf);
                wrote = true;
            }
            if (wrote) {
                byte[] obuf = ci.doFinal();
                if (obuf != null) out.write(obuf);
            }
        } catch (SocketException e) {
            throw e;
        } catch (Throwable e) {
            throw new EncryptionException(e, "Could not decrypt data");
        }
    }

    @Override
    public NamedContainer<net.ME1312.SubData.Server.Cipher, String> newCipher(String handle) {
        return forwardG.getOrDefault(handle.toUpperCase(), () -> null).run();
    }

    @Override
    public net.ME1312.SubData.Server.Cipher getCipher(String handle, String key) {
        return forwardP.getOrDefault(handle.toUpperCase(), token -> null).run(key);
    }

    public static void addCipher(String handle, ReturnRunnable<NamedContainer<net.ME1312.SubData.Server.Cipher, String>> generator, ReturnCallback<String, net.ME1312.SubData.Server.Cipher> parser) {
        if (Util.isNull(generator)) throw new NullPointerException();
        handle = handle.toUpperCase();
        if (!forwardG.keySet().contains(handle)) forwardG.put(handle, generator);
        if (!forwardP.keySet().contains(handle)) forwardP.put(handle, parser);
    }

    public static void removeCipher(String handle) {
        forwardG.remove(handle.toUpperCase());
        forwardP.remove(handle.toUpperCase());
    }
}
