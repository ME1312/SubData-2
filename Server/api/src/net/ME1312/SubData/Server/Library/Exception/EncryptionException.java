package net.ME1312.SubData.Server.Library.Exception;

import java.io.IOException;

/**
 * Encryption Exception
 */
public class EncryptionException extends IOException {
    public EncryptionException() {}
    public EncryptionException(String s) {
        super(s);
    }
    public EncryptionException(Throwable e, String s) {
        super(s, e);
    }
    public EncryptionException(Throwable e) {
        super(e);
    }
}
