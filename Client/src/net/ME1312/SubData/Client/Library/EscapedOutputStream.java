package net.ME1312.SubData.Client.Library;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Escaped OutputStream Class
 */
public class EscapedOutputStream extends OutputStream {
    private final int escape;
    private final int[] controls;
    private final OutputStream out;
    private boolean escaped = false;

    /**
     * Creates an OutputStream with escaped controls
     *
     * @param stream Super stream
     * @param escape Escape data
     * @param controls Control data
     */
    public EscapedOutputStream(OutputStream stream, int escape, int... controls) {
        Arrays.sort(controls);

        this.out = stream;
        this.escape = escape;
        this.controls = controls;
    }

    /**
     * Write data to the stream
     *
     * @param b Data
     * @throws IOException
     */
    @Override
    public void write(int b) throws IOException {
        if (escaped) {
            if (b == escape || Arrays.binarySearch(controls, b) >= 0) {
                out.write(escape);
            }
            escaped = false;
        }
        if (b == escape) {
            escaped = true;
        }
        out.write(b);
    }

    /**
     * Write control data to the stream
     *
     * @param b Control Data
     * @throws IOException
     */
    public void control(int b) throws IOException {
        if (Arrays.binarySearch(controls, b) < 0)
            throw new IllegalArgumentException("Character " + DebugUtil.toHex(0xFFFF, b) + " is not a control character");

        if (escaped) {
            out.write(escape);
            escaped = false;
        }
        out.write(escape);
        out.write(b);
    }

    /**
     * Flush the stream
     *
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Close the stream
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        out.close();
    }
}
