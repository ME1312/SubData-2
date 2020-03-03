package net.ME1312.SubData.Client.Encryption;

import net.ME1312.Galaxi.Library.Callback.ReturnCallback;
import net.ME1312.Galaxi.Library.Callback.ReturnRunnable;
import net.ME1312.Galaxi.Library.Container.NamedContainer;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Client.*;
import net.ME1312.SubData.Client.Library.EscapedOutputStream;
import net.ME1312.SubData.Client.Library.Exception.EncryptionException;

import javax.crypto.KeyAgreement;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

/**
 * Diffie-Hellman Exchange Handler Class (agrees upon and uses an AES encryption key)
 */
public class DHE implements Cipher, CipherFactory {

    // Supported Forward Ciphers
    private static final HashMap<String, ReturnRunnable<NamedContainer<Cipher, String>>> forwardG = new HashMap<String, ReturnRunnable<NamedContainer<Cipher, String>>>();
    private static final HashMap<String, ReturnCallback<String, Cipher>> forwardP = new HashMap<String, ReturnCallback<String, Cipher>>();

    // Cipher Properties
    private static final int REFRESH = 125;

    // Hold Data for use by SubData Cipher methods
    private static final HashMap<Integer, DHE> instances = new HashMap<Integer, DHE>();
    private final HashMap<DataClient, DHE.Data> data = new HashMap<DataClient, DHE.Data>();
    private final int keyLength;

    static {
        addCipher("AES", () -> AES.random(128), key -> new AES(128, key));
        addCipher("AES-128", () -> AES.random(128), key -> new AES(128, key));
        addCipher("AES-192", () -> AES.random(192), key -> new AES(192, key));
        addCipher("AES-256", () -> AES.random(256), key -> new AES(256, key));
    }

    public String getName() {
        return "DHE-" + keyLength;
    }

    /**
     * Get the DHE instance
     *
     * @return DHE
     */
    public static DHE get(int keyLength) {
        if (!instances.keySet().contains(keyLength)) instances.put(keyLength, new DHE(keyLength));
        return instances.get(keyLength);
    }
    private DHE(int keyLength) {
        this.keyLength = keyLength;
    }
    private final class Data {
        private PublicKey key;
        private KeyAgreement agreement;
        private ByteArrayOutputStream data;
        private boolean sent, received;
        private Cipher next;

        private Data() throws EncryptionException {
            KeyPairGenerator kpg;
            try {
                kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(keyLength * 2);
                KeyPair kp = kpg.generateKeyPair();
                key = kp.getPublic();
                agreement = KeyAgreement.getInstance("ECDH");
                agreement.init(kp.getPrivate());
            } catch (Throwable e) {
                throw new EncryptionException(e);
            }
        }

        private void compile() throws EncryptionException {
            received = true;
            try {
                agreement.doPhase(KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(data.toByteArray())), true);

                StringBuilder builder = new StringBuilder();
                byte[] ba = agreement.generateSecret();
                ByteBuffer buf = ByteBuffer.wrap(ba);
                int i = ba.length;
                while (i > 3) {
                    builder.append((char) buf.getInt());
                    i -= 4;
                }
                next = new AES(keyLength, builder.toString());
            } catch (Throwable e) {
                throw new EncryptionException(e);
            }
        }
    }

    @Override
    public void encrypt(DataClient client, InputStream in, OutputStream out) throws Exception {
        DHE.Data data = this.data.get(client);
        if (data == null) {
            data = new DHE.Data();
            this.data.put(client, data);
        }

        if (!data.sent) {
            try {
                EscapedOutputStream stream = new EscapedOutputStream(out, '\u0010', '\u000E', '\u000F');
                stream.control('\u000E');
                stream.write(data.key.getEncoded());
                stream.control('\u000F');
                data.sent = true;
            } catch (Throwable e) {
                throw new EncryptionException(e);
            }
        }

        while (data.next == null) {
            Thread.sleep(REFRESH);
        }

        data.next.encrypt(client, in, out);
    }

    @Override
    public void decrypt(DataClient client, InputStream in, OutputStream out) throws Exception {
        DHE.Data data = this.data.get(client);
        if (data == null) {
            data = new DHE.Data();
            this.data.put(client, data);
        }

        boolean escaped = false, receiving = false;
        int b;
        while (!data.received && (b = in.read()) != -1) {
            if (escaped) {
                switch (b) {
                    case '\u0010': // [DLE] (Escape character)
                        if (receiving) data.data.write('\u0010');
                        break;
                    case '\u000E': //  [SO] (L2 Handshake Begin character)
                        data.data = new ByteArrayOutputStream();
                        receiving = true;
                        break;
                    case '\u000F': //  [SI] (L2 Handshake End character)
                        data.compile();
                        break;
                    default:
                        if (receiving) {
                            data.data.write('\u0010');
                            data.data.write(b);
                        }
                        break;
                }
                escaped = false;
            } else if (b == '\u0010') {
                escaped = true;
            } else if (receiving) {
                data.data.write(b);
            }
        }

        if (data.received) {
            while (data.next == null) {
                Thread.sleep(REFRESH);
            }

            data.next.decrypt(client, in, out);
        }
    }

    @Override
    public void retire(DataClient client) {
        data.remove(client);
    }

    @Override
    public NamedContainer<Cipher, String> newCipher(String handle) {
        return forwardG.getOrDefault(handle.toUpperCase(), () -> null).run();
    }

    @Override
    public Cipher getCipher(String handle, String key) {
        return forwardP.getOrDefault(handle.toUpperCase(), token -> null).run(key);
    }

    public static void addCipher(String handle, ReturnRunnable<NamedContainer<Cipher, String>> generator, ReturnCallback<String, Cipher> parser) {
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
