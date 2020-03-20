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
    private final InputStream in;
    private final InputStream unescaped;
    private byte[][][][] blocks = null;
    private int gb, mb, kb, b;
    private long stored = 0;
    private boolean unescaping;
    private boolean open = true;
    private boolean finished = false;

    /**
     * Creates a SubData Layer 1 InputStream
     *
     * @param stream Super stream
     * @param reset Read Reset action
     * @param close End of Packet action
     */
    public InputStreamL1(InputStream stream, Runnable reset, Runnable close) {
        this.in = stream;
        this.reset = reset;
        this.close = close;
        this.unescaped = new InputStream() {
            Integer pending = null;

            @Override
            public int read() throws IOException {
                int b = (pending != null)?pending:in.read();
                pending = null;

                if (b == -1) {
                    throw new EndOfStreamException();
                } else if (b == '\u0010') {
                    int next = in.read();
                    if (next == '\u0010') {        // [DLE] (Escape)
                        /* no action necessary */
                    } else if (next == '\u0017') { // [ETB] (End of Data)
                        unescaping = false;
                        b = InputStreamL1.this.next();
                    } else {
                        pending = next;
                    }
                }
                return b;
            }
        };
    }

    private int raw() throws IOException {
        return in.read();
    }

    private int next() throws IOException {
        int b = raw();

        if (b == '\u0010') {        // [DLE] Escaped Data
            unescaping = true;
            return unescaped.read();
        } else if (b == '\u0011') { // [DC1] Raw Byte(s) of Data
            this.blocks = new byte[1][1][1][(raw() + 1) * 4];
            for (int i = 0; i < blocks[0][0][0].length; ++i)
                blocks[0][0][0][i] = (byte) raw();
            return store(blocks[0][0][0].length);
        } else if (b == '\u0012') { // [DC2] Raw KB(s) of Data
            this.blocks = new byte[1][1][(raw() + 1) * 4][1024];
            for (int i = 0; i < blocks[0][0].length; ++i)
                for (int ii = 0; ii < blocks[0][0][i].length; ++ii)
                    blocks[0][0][i][ii] = (byte) raw();
            return store(blocks[0][0].length * KB);
        } else if (b == '\u0013') { // [DC3] Raw MB(s) of Data
            this.blocks = new byte[1][(raw() + 1) * 4][1024][1024];
            for (int i = 0; i < blocks[0].length; ++i)
                for (int ii = 0; ii < blocks[0][i].length; ++ii)
                    for (int iii = 0; iii < blocks[0][i][ii].length; ++iii)
                        blocks[0][i][ii][iii] = (byte) raw();
            return store(blocks[0].length * MB);
        } else if (b == '\u0014') { // [DC4] Raw GB(s) of Data
            this.blocks = new byte[(raw() + 1) * 4][1024][1024][1024];
            for (int i = 0; i < blocks.length; ++i)
                for (int ii = 0; ii < blocks[i].length; ++ii)
                    for (int iii = 0; iii < blocks[i][ii].length; ++iii)
                        for (int iiii = 0; iiii < blocks[i][ii][iii].length; ++iiii)
                            blocks[i][ii][iii][iiii] = (byte) raw();
            return store(blocks.length * GB);
        } else if (b == '\u0018') { // [CAN] Read Reset
            reset.run();
            finished = true;
            throw new EndOfStreamException();
        } else if (b == '\u0017') { // [ETB] End of Packet
            finished = true;
            throw new EndOfStreamException();
        } else if (b == -1) {
            throw new EndOfStreamException();
        }
        return b;
    }

    private int store(long amount) {
        this.gb = 0;
        this.mb = 0;
        this.kb = 0;
        this.b  = 1;
        this.stored = amount - 1;
        return Byte.toUnsignedInt(blocks[0][0][0][0]);
    }

    @Override
    public int read() throws IOException {
        if (open) {
            if (stored > 0) {
                byte b = blocks[gb][mb][kb][this.b++];
                --stored;

                if (this.b >= 1024) {
                    this.b = 0;
                    ++kb;
                    if (kb >= 1024) {
                        kb = 0;
                        ++mb;
                        if (mb >= 1024) {
                            mb = 0;
                            ++gb;
                        }
                    }
                }

                return Byte.toUnsignedInt(b);
            } else {
                try {
                    if (unescaping) {
                        return unescaped.read();
                    } else {
                        return next();
                    }
                } catch (EndOfStreamException e) {
                    close();
                    return -1;
                }
            }
        } else return -1;
    }

    @Override
    public void close() throws IOException {
        if (open) {
            open = false;
            if (!finished) {
                while (next() != -1);
            }
            close.run();
        }
    }
}
