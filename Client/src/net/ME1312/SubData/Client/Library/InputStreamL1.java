package net.ME1312.SubData.Client.Library;

import net.ME1312.SubData.Client.Library.Exception.EndOfStreamException;

import java.io.IOException;
import java.io.InputStream;

import static net.ME1312.SubData.Client.Library.DataSize.*;

/**
 * SubData Layer 1 InputStream Class
 */
public class InputStreamL1 extends InputStream {
    private final Runnable reset, close;
    private final Skip skip = new Skip();
    private final InputStream raw, control, escaped, closed;
    private InputStream redirect;
    private boolean open = true;

    private class Skip extends InputStream {
        private long max, i;

        private int start(long amount) throws IOException {
            i = 0;
            max = amount;
            redirect = skip;
            return read();
        }

        @Override
        public int read() throws IOException {
            if (++i >= max) redirect = control;
            return raw.read();
        }
    }

    public InputStreamL1(InputStream stream, Runnable reset, Runnable close) {
        this.reset = reset;
        this.close = close;
        this.raw = stream;
        this.control = redirect = new InputStream() {
            @Override
            public int read() throws IOException {
                int b = raw.read();

                switch (b) {
                    case '\u0014': // [DC4] Raw GB(s) of Data
                        return skip.start((raw.read() + 1) * GBB);
                    case '\u0013': // [DC3] Raw MB(s) of Data
                        return skip.start((raw.read() + 1) * MBB);
                    case '\u0012': // [DC2] Raw KB(s) of Data
                        return skip.start((raw.read() + 1) * KBB);
                    case '\u0011': // [DC1] Raw Byte(s) of Data
                        return skip.start((raw.read() + 1) *  BB);
                    case '\u0010': // [DLE] Escaped Data
                        return (redirect = escaped).read();
                    case '\u0018': // [CAN] Read Reset
                        reset.run();
                    case '\u0017': // [ETB] End of Packet
                        InputStreamL1.this.close();
                        return -1;
                    case -1:
                        InputStreamL1.this.close();
                        throw new EndOfStreamException();
                    default:
                        return b;
                }
            }
        };
        this.escaped = new InputStream() {
            Integer pending = null;

            @Override
            public int read() throws IOException {
                int b = (pending != null)?pending:raw.read();
                pending = null;

                if (b == -1) {
                    InputStreamL1.this.close();
                    throw new EndOfStreamException();
                } else if (b == '\u0010') {
                    int next = raw.read();
                    if (next == '\u0010') {        // [DLE] (Escape)
                        /* no action necessary */
                    } else if (next == '\u0017') { // [ETB] (End of Data)
                        b = (redirect = control).read();
                    } else {
                        pending = next;
                    }
                }
                return b;
            }
        };
        this.closed = new InputStream() {
            @Override
            public int read() throws IOException {
                return -1;
            }
        };
    }

    @Override
    public int read() throws IOException {
        return redirect.read();
    }

    @Override
    public void close() throws IOException {
        if (open) {
            open = false;
            redirect = closed;
            close.run();
        }
    }
}
