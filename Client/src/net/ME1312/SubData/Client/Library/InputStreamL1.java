package net.ME1312.SubData.Client.Library;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import static net.ME1312.SubData.Client.Library.DataSize.*;

/**
 * SubData Layer 1 InputStream Class
 */
public class InputStreamL1 {
    private final Runnable shutdown;
    private final Consumer<Integer> error;
    private final InputStream in;
    private Runnable reset, close;
    private DataInterface open;

    public InputStreamL1(InputStream in, Runnable shutdown, Consumer<Integer> error) {
        this.in = in;
        this.shutdown = shutdown;
        this.error = error;
    }

    public InputStream open(Runnable reset, Runnable close) {
        if (open != null) open.shutdown();
        this.reset = reset;
        this.close = close;
        return open = new DataInterface();
    }

    private final class DataInterface extends InputStream {
        private int permitted = 0;

        @Override
        public int read(byte[] data, int offset, int length) throws IOException {
            if (open == this) {
                int transferred, total = 0;
                for (;;) {
                    transferred = in.read(data, offset, Math.min(length, permitted));
                    if (transferred == -1) {
                        shutdown.run();
                        if (total == 0) return -1;
                        return total;
                    }
                    total += transferred;
                    permitted -= transferred;
                    if (transferred == length || permitted != 0) {
                        return total;
                    }
                    if (permit()) {
                        if (total == 0) return -1;
                        return total;
                    }
                    offset += transferred;
                    length -= transferred;
                }
            } else return -1;
        }

        @Override
        public int read() throws IOException {
            for (;;) {
                if (permitted != 0) {
                    int b = in.read();
                    if (b == -1) {
                        shutdown.run();
                        return -1;
                    } else {
                        --permitted;
                        return b;
                    }
                } else {
                    if (permit()) {
                        return -1;
                    }
                }
            }
        }

        @Override
        public void close() throws IOException {
            while (read() != -1);
        }

        private void shutdown() {
            open = null;
            permitted = 0;
        }

        private boolean permit() throws IOException {
            if (open == this) {
                int b = in.read();
                for (;;) {
                    switch (b) {
                     /* case '\u0014': // [DC4] Raw GB Block(s) of Data
                            permitted = (in.read() + 1) * GBB; // Impossible with 32-bit signed int :(
                            return false; */
                        case '\u0013': // [DC3] Raw MB Block(s) of Data
                            permitted = (in.read() + 1) * MBB;
                            return false;
                        case '\u0012': // [DC2] Raw KB Block(s) of Data
                            permitted = (in.read() + 1) * KBB;
                            return false;
                        case '\u0011': // [DC1] Raw Byte Block(s) of Data
                            permitted = (in.read() + 1) *  BB;
                            return false;
                        case '\u0010': // [DLE] Raw Byte of Data
                            permitted = 1;
                            return false;
                        case '\u0018': // [CAN] Read Reset
                            reset.run();
                        case '\u0017': // [ETB] End of Packet
                            shutdown();
                            close.run();
                            return true;
                        case  0:
                            break;
                        case -1:
                            shutdown.run();
                            return true;
                        default:
                            error.accept(b);
                            return true;
                    }
                    b = in.read();
                }
            } else return true;
        }
    }

    public void shutdown() {
        if (open != null) {
            reset.run();
            open.shutdown();
        }
    }
}