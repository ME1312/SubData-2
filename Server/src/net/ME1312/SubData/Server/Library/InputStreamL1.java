package net.ME1312.SubData.Server.Library;

import java.io.IOException;
import java.io.InputStream;

import static net.ME1312.SubData.Server.Library.DataSize.*;

/**
 * SubData Layer 1 InputStream Class
 */
public class InputStreamL1 {
    private final Runnable shutdown;
    private final InputStream in;
    private Runnable reset, close;
    private DataInterface open;

    public InputStreamL1(InputStream in, Runnable shutdown) {
        this.in = in;
        this.shutdown = shutdown;
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
            if (offset < 0 || length < 0 || length > data.length - offset) {
                throw new IndexOutOfBoundsException();
            }

            if (open == this) {
                int total = 0;
                int transferred;
                do {
                    total += transferred = in.read(data, offset, Math.min(length, permitted));
                    if (transferred == -1) {
                        shutdown.run();
                        break;
                    }
                    permitted -= transferred;
                    if (transferred == length) break;
                    if (permit()) break;
                    offset += transferred;
                    length -= transferred;
                } while (true);
                return total;
            } else return -1;
        }

        @Override
        public int read() throws IOException {
            do {
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
                    if (permit()) return -1;
                }
            } while (true);
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
                do {
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
                            return true;
                        case '\u0017': // [ETB] End of Packet
                            shutdown();
                            close.run();
                            return true;
                        case -1:
                            shutdown.run();
                            return true;
                    }

                    b = in.read();
                } while (true);
            } else return true;
        }
    }

    public void shutdown() {
        if (open != null) {
            open.shutdown();
        }
    }
}