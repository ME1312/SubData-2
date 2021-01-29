package net.ME1312.SubData.Server.Library;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static net.ME1312.SubData.Server.Library.DataSize.*;

/**
 * SubData Layer 1 OutputStream Class
 */
public class OutputStreamL1 extends OutputStream {
    private final ExecutorService writer;
    private final Logger log;
    private final Runnable shutdown;
    private final OutputStream out;
    private byte[] block;
    private int cursor = 0;
    public int limit;

    public OutputStreamL1(Logger log, OutputStream stream, int limit, Runnable shutdown, String name) {
        this.writer = Executors.newSingleThreadExecutor(r -> new Thread(r, name));
        this.shutdown = shutdown;
        this.log = log;
        this.out = stream;
        this.limit = limit;
        this.block = new byte[limit];
    }

    public void resize(int limit) {
        if (this.limit != limit) {
            this.limit = limit;
            if (cursor >= limit) {
                flush();
            } else {
                final byte[] block = this.block;
                this.block = new byte[limit];
                System.arraycopy(block, 0, this.block, 0, cursor);
            }
        }
    }

    @Override
    public void write(byte[] data, int offset, int length) {
        int transferred;
        do {
            System.arraycopy(data, offset, block, cursor, transferred = Math.min(length, block.length - cursor));
            cursor += transferred;
            if (transferred == length) {
                break;
            }
            flush();
            offset += transferred;
            length -= transferred;
        } while (true);

        if (cursor == block.length) flush();
    }

    @Override
    public void write(int b) {
        block[cursor++] = (byte) b;
        if (cursor == block.length) flush();
    }

    @Override
    public void flush() {
        if (cursor != 0 && !writer.isShutdown()) {
            if (cursor == block.length || limit != block.length) {
                writer.submit(new DataFlusher(block, cursor)::flush);
                block = new byte[limit];
            } else {
                final byte[] block = new byte[cursor];
                System.arraycopy(this.block, 0, block, 0, cursor);
                writer.submit(new DataFlusher(block, cursor)::flush);
            }
            cursor = 0;
        }
    }

    public void control(int b) {
        if (!writer.isShutdown()) writer.submit(() -> {
            try {
                out.write(b);
                out.flush();
            } catch (IOException e) {
                if (!(e instanceof SocketException)) {
                    DebugUtil.logException(e, log);
                } else shutdown.run();
            }
        });
    }

    private final class DataFlusher {
        private final byte[] block;
        private int stored, cursor = 0;

        private DataFlusher(byte[] data, int length) {
            this.stored = length;
            this.block = data;
        }

        private void flush() {
            try {
                while (stored > 0) {
                    if (stored >= MBB) {
                        stored -= flushMBB();
                    } else if (stored >= KBB) {
                        stored -= flushKBB();
                    } else if (stored >= BB) {
                        stored -= flushBB();
                    } else {
                        stored -= flushByte();
                    }
                }
            } catch (IOException e) {
                if (!(e instanceof SocketException)) {
                    DebugUtil.logException(e, log);
                } else shutdown.run();
            }
        }
        private long flushMBB() throws IOException {
            int size = Math.min((int) Math.floor((double) stored / MBB), 256);
            int length = size * MBB;
            out.write('\u0013');
            out.write((byte) (size - 1));
            out.write(block, cursor, length);
            cursor += length;
            return length;
        }
        private long flushKBB() throws IOException {
            int size = Math.min((int) Math.floor((double) stored / KBB), 256);
            int length = size * KBB;
            out.write('\u0012');
            out.write((byte) (size - 1));
            out.write(block, cursor, length);
            cursor += length;
            return length;
        }
        private long flushBB() throws IOException {
            int size = Math.min((int) Math.floor((double) stored /  BB), 256);
            int length = size *  BB;
            out.write('\u0011');
            out.write((byte) (size - 1));
            out.write(block, cursor, length);
            cursor += length;
            return length;
        }
        private long flushByte() throws IOException {
            out.write('\u0010');
            out.write(block[cursor++]);
            return 1;
        }
    }

    public void shutdown() {
        writer.shutdown();
    }
}