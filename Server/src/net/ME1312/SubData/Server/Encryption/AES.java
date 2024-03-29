package net.ME1312.SubData.Server.Encryption;

import net.ME1312.Galaxi.Library.Container.ContainedPair;
import net.ME1312.Galaxi.Library.Container.Pair;
import net.ME1312.SubData.Server.DataClient;
import net.ME1312.SubData.Server.Library.Exception.EncryptionException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Random;

/**
 * A class to perform password-based AES encryption and decryption in CBC mode.
 * 128, 192, and 256-bit encryption are supported, provided that the latter two
 * are permitted by the Java runtime's jurisdiction policy files.
 * <br/>
 * The public interface for this class consists of the static methods
 * {@link #encrypt} and {@link #decrypt}, which encrypt and decrypt arbitrary
 * streams of data, respectively.
 *
 * @author dweymouth@gmail.com
 */
public final class AES implements net.ME1312.SubData.Server.Cipher {

    // AES specification
    private static final String CIPHER_SPEC = "AES/CBC/PKCS5Padding";

    // Key derivation specification
    private static final String KEYGEN_SPEC = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16; // in bytes
    private static final int AUTH_KEY_LENGTH = 8; // in bytes
    private static final int ITERATIONS = 32768;

    // Process input/output streams in chunks
    private static final int BUFFER_SIZE = 1024;

    // Hold Data for use by SubData Cipher methods
    private final int keyLength;
    private final String key;

    /**
     * Initialize AES Cipher
     *
     * @param keyLength 128, 192, or 256 bit mode
     * @param key Password
     */
    public AES(int keyLength, String key) {
        if (keyLength != 128 && keyLength != 192 && keyLength != 256) throw new IllegalArgumentException(Integer.toString(keyLength));
        this.keyLength = keyLength;
        this.key = key;
    }

    /**
     * Initialize Random AES Cipher
     *
     * @param keyLength 128, 192, or 256 bit mode
     */
    static Pair<net.ME1312.SubData.Server.Cipher, String> random(int keyLength) {
        StringBuilder builder = new StringBuilder();
        SecureRandom random = new SecureRandom();
        int i = random.nextInt(keyLength + 1) + keyLength;
        while (i > 0) {
            builder.append((char) random.nextInt(Character.MAX_VALUE + 1));
            i -= 16 / 2; // char uses 16 bytes
        }

        String key = builder.toString();
        return new ContainedPair<>(new AES(keyLength, key), key);
    }

    /**
     * @return a new pseudorandom salt of the specified length
     */
    private static byte[] generateSalt() {
        Random r = new SecureRandom();
        byte[] salt = new byte[AES.SALT_LENGTH];
        r.nextBytes(salt);
        return salt;
    }

    /**
     * Derive an AES encryption key and authentication key from given password and salt,
     * using PBKDF2 key stretching. The authentication key is 64 bits long.
     * @param keyLength
     *   length of the AES key in bits (128, 192, or 256)
     * @param password
     *   the password from which to derive the keys
     * @param salt
     *   the salt from which to derive the keys
     * @return a Keys object containing the two generated keys
     */
    private static Keys keygen(int keyLength, char[] password, byte[] salt) {
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance(KEYGEN_SPEC);
        } catch (NoSuchAlgorithmException impossible) { return null; }
        // derive a longer key, then split into AES key and authentication key
        KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, keyLength + AUTH_KEY_LENGTH * 8);
        SecretKey tmp = null;
        try {
            tmp = factory.generateSecret(spec);
        } catch (InvalidKeySpecException impossible) { }
        byte[] fullKey = tmp.getEncoded();
        SecretKey authKey = new SecretKeySpec( // key for password authentication
                Arrays.copyOfRange(fullKey, 0, AUTH_KEY_LENGTH), "AES");
        SecretKey encKey = new SecretKeySpec( // key for AES encryption
                Arrays.copyOfRange(fullKey, AUTH_KEY_LENGTH, fullKey.length), "AES");
        return new Keys(encKey, authKey);
    }

    /**
     * Encrypts a stream of data. The encrypted stream consists of a header
     * followed by the raw AES data. The header is broken down as follows:<br/>
     * <ul>
     *   <li><b>keyLength</b>: AES key length in bytes (valid for 16, 24, 32) (1 byte)</li>
     *   <li><b>salt</b>: pseudorandom salt used to derive keys from password (16 bytes)</li>
     *   <li><b>authentication key</b> (derived from password and salt, used to
     *     check validity of password upon decryption) (8 bytes)</li>
     *   <li><b>IV</b>: pseudorandom AES initialization vector (16 bytes)</li>
     * </ul>
     *
     * @param keyLength
     *   key length to use for AES encryption (must be 128, 192, or 256)
     * @param password
     *   password to use for encryption
     * @param input
     *   an arbitrary byte stream to encrypt
     * @param output
     *   stream to which encrypted data will be written
     * @throws AES.InvalidKeyLengthException
     *   if keyLength is not 128, 192, or 256
     * @throws AES.StrongEncryptionNotAvailableException
     *   if keyLength is 192 or 256, but the Java runtime's jurisdiction
     *   policy files do not allow 192- or 256-bit encryption
     * @throws IOException
     */
    public static void encrypt(int keyLength, String password, InputStream input, OutputStream output)
            throws InvalidKeyLengthException, StrongEncryptionNotAvailableException, IOException {
        // Check validity of key length
        if (keyLength != 128 && keyLength != 192 && keyLength != 256) {
            throw new InvalidKeyLengthException(keyLength);
        }

        // generate salt and derive keys for authentication and encryption
        byte[] salt = generateSalt();
        Keys keys = keygen(keyLength, password.toCharArray(), salt);

        // initialize AES encryption
        Cipher encrypt = null;
        try {
            encrypt = Cipher.getInstance(CIPHER_SPEC);
            encrypt.init(Cipher.ENCRYPT_MODE, keys.encryption);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException impossible) { }
        catch (InvalidKeyException e) { // 192 or 256-bit AES not available
            throw new StrongEncryptionNotAvailableException(keyLength);
        }

        // get initialization vector
        byte[] iv = null;
        try {
            iv = encrypt.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
        } catch (InvalidParameterSpecException impossible) { }

        // write authentication and AES initialization data
        output.write(keyLength / 8);
        output.write(salt);
        output.write(keys.authentication.getEncoded());
        output.write(iv);

        // read data from input into buffer, encrypt and write to output
        byte[] buffer = new byte[BUFFER_SIZE];
        int numRead;
        byte[] encrypted = null;
        while ((numRead = input.read(buffer)) > 0) {
            encrypted = encrypt.update(buffer, 0, numRead);
            if (encrypted != null) {
                output.write(encrypted);
            }
        }
        try { // finish encryption - do final block
            encrypted = encrypt.doFinal();
        } catch (IllegalBlockSizeException | BadPaddingException impossible) { }
        if (encrypted != null) {
            output.write(encrypted);
        }
        output.flush();
    }

    @Override
    public void encrypt(DataClient client, InputStream in, OutputStream out) throws Exception {
        encrypt(keyLength, key, in, out);
    }

    public String getName() {
        return "AES-" + keyLength;
    }

    /**
     * Decrypts a stream of data that was encrypted by {@link #encrypt}.
     * @param password
     *   the password used to encrypt/decrypt the stream
     * @param input
     *   stream of encrypted data to be decrypted
     * @param output
     *   stream to which decrypted data will be written
     * @return the key length for the decrypted stream (128, 192, or 256)
     * @throws AES.InvalidPasswordException
     *   if the given password was not used to encrypt the data
     * @throws AES.InvalidAESStreamException
     *   if the given input stream is not a valid AES-encrypted stream
     * @throws AES.StrongEncryptionNotAvailableException
     *   if the stream is 192 or 256-bit encrypted, and the Java runtime's
     *   jurisdiction policy files do not allow for AES-192 or 256
     * @throws IOException
     */
    public static int decrypt(String password, InputStream input, OutputStream output)
            throws InvalidPasswordException, InvalidAESStreamException, IOException,
            StrongEncryptionNotAvailableException {
        int keyLength = input.read() * 8;
        // Check validity of key length
        if (keyLength != 128 && keyLength != 192 && keyLength != 256) {
            throw new InvalidAESStreamException();
        }

        // read salt, generate keys, and authenticate password
        byte[] salt = new byte[SALT_LENGTH];
        input.read(salt);
        Keys keys = keygen(keyLength, password.toCharArray(), salt);
        byte[] authRead = new byte[AUTH_KEY_LENGTH];
        input.read(authRead);
        if (!Arrays.equals(keys.authentication.getEncoded(), authRead)) {
            throw new InvalidPasswordException();
        }

        // initialize AES decryption
        byte[] iv = new byte[16]; // 16-byte I.V. regardless of key size
        input.read(iv);
        Cipher decrypt = null;
        try {
            decrypt = Cipher.getInstance(CIPHER_SPEC);
            decrypt.init(Cipher.DECRYPT_MODE, keys.encryption, new IvParameterSpec(iv));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidAlgorithmParameterException impossible) { }
        catch (InvalidKeyException e) { // 192 or 256-bit AES not available
            throw new StrongEncryptionNotAvailableException(keyLength);
        }

        // read data from input into buffer, decrypt and write to output
        byte[] buffer = new byte[BUFFER_SIZE];
        int numRead;
        byte[] decrypted;
        while ((numRead = input.read(buffer)) > 0) {
            decrypted = decrypt.update(buffer, 0, numRead);
            if (decrypted != null) {
                output.write(decrypted);
            }
        }
        try { // finish decryption - do final block
            decrypted = decrypt.doFinal();
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new InvalidAESStreamException(e);
        }
        if (decrypted != null) {
            output.write(decrypted);
        }

        output.flush();
        return keyLength;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void decrypt(DataClient client, InputStream in, OutputStream out) throws Exception {
        decrypt(key, in, out);
    }

    /**
     * A tuple of encryption and authentication keys returned by {@link #keygen}
     */
    private static class Keys {
        public final SecretKey encryption, authentication;
        public Keys(SecretKey encryption, SecretKey authentication) {
            this.encryption = encryption;
            this.authentication = authentication;
        }
    }

    /**
     * Thrown if an attempt is made to decrypt a stream with an incorrect password.
     */
    public static class InvalidPasswordException extends EncryptionException { }

    /**
     * Thrown if an attempt is made to encrypt a stream with an invalid AES key length.
     */
    public static class InvalidKeyLengthException extends EncryptionException {
        InvalidKeyLengthException(int length) {
            super("Invalid AES key length: " + length);
        }
    }

    /**
     * Thrown if 192- or 256-bit AES encryption or decryption is attempted,
     * but not available on the particular Java platform.
     */
    public static class StrongEncryptionNotAvailableException extends EncryptionException {
        public StrongEncryptionNotAvailableException(int keySize) {
            super(keySize + "-bit AES encryption is not available on this Java platform.");
        }
    }

    /**
     * Thrown if an attempt is made to decrypt an invalid AES stream.
     */
    public static class InvalidAESStreamException extends EncryptionException {
        public InvalidAESStreamException() { super(); };
        public InvalidAESStreamException(Exception e) { super(e); }
    }

}