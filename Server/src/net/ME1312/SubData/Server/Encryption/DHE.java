package net.ME1312.SubData.Server.Encryption;

import net.ME1312.Galaxi.Library.Callback.ReturnCallback;
import net.ME1312.Galaxi.Library.Callback.ReturnRunnable;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubData.Server.Cipher;
import net.ME1312.SubData.Server.CipherFactory;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.Library.EscapedOutputStream;
import net.ME1312.SubData.Server.Library.Exception.EncryptionException;
import net.ME1312.SubData.Server.Library.OutputStreamL1;
import net.ME1312.SubData.Server.Protocol.Internal.PacketNull;
import net.ME1312.SubData.Server.SubDataClient;

import javax.crypto.KeyAgreement;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

/**
 * Diffie-Hellman Exchange Handler Class (agrees upon and uses an AES encryption key)
 */
public class DHE implements Cipher, CipherFactory {

    // Supported Forward Ciphers
    private static final HashMap<String, ReturnRunnable<Pair<Cipher, String>>> forwardG = new HashMap<String, ReturnRunnable<Pair<Cipher, String>>>();
    private static final HashMap<String, ReturnCallback<String, Cipher>> forwardP = new HashMap<String, ReturnCallback<String, Cipher>>();

    // Cipher Properties
    private static final int REFRESH = 125;

    // Hold Data for use by SubData Cipher methods
    private static final HashMap<Integer, DHE> instances = new HashMap<Integer, DHE>();
    private final HashMap<DataClient, Data> data = new HashMap<DataClient, Data>();
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
        if (keyLength != 128 && keyLength != 192 && keyLength != 256) throw new IllegalArgumentException(Integer.toString(keyLength));
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
        private boolean sent, initSent, received;
        private Cipher next;

        private Data() throws EncryptionException {
            KeyPairGenerator kpg;
            try {
                kpg = KeyPairGenerator.getInstance("EC");
                ECGenParameterSpec spec;
                if (keyLength >= 256) {
                    spec = new ECGenParameterSpec("secp521r1");
                } else if (keyLength >= 192) {
                    spec = new ECGenParameterSpec("secp384r1");
                } else {
                    spec = new ECGenParameterSpec("secp256r1");
                }
                kpg.initialize(spec);
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
                while (i > 1) {
                    builder.append(buf.getChar());
                    i -= 2; // char uses 16 bits (2 bytes)
                }

                String key = builder.toString();
                next = new AES(keyLength, key);
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
                EscapedOutputStream stream = new EscapedOutputStream(out, '\u001B', '\u000E', '\u000F');
                stream.control('\u000E');
                stream.write(data.key.getEncoded());
                stream.control('\u000F');
                Util.<OutputStreamL1>reflect(SubDataClient.class.getDeclaredField("out"), client).flush();
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
                    case '\u001B': // [ESC] (Escape character)
                        if (receiving) data.data.write('\u001B');
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
                            data.data.write('\u001B');
                            data.data.write(b);
                        }
                        break;
                }
                escaped = false;
            } else if (b == '\u001B') {
                escaped = true;
            } else if (receiving) {
                data.data.write(b);
            }
        }

        if (data.received) {
            if (!data.initSent) {
                data.initSent = true;
                OutputStreamL1 stream = Util.reflect(SubDataClient.class.getDeclaredField("out"), client);
                stream.control('\u0018');
                stream.flush();
                ((SubDataClient) client).sendPacket(new PacketNull());
            }
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
    public Pair<Cipher, String> newCipher(String handle) {
        return forwardG.getOrDefault(handle.toUpperCase(), () -> null).run();
    }

    @Override
    public Cipher getCipher(String handle, String key) {
        return forwardP.getOrDefault(handle.toUpperCase(), token -> null).run(key);
    }

    public static void addCipher(String handle, ReturnRunnable<Pair<Cipher, String>> generator, ReturnCallback<String, Cipher> parser) {
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
