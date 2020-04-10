package net.ME1312.SubData.Server.Library;

import net.ME1312.Galaxi.Library.Callback.ExceptionRunnable;
import net.ME1312.Galaxi.Library.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Logger;

import static net.ME1312.SubData.Server.Library.DataSize.*;

/**
 * SubData Layer 1 OutputStream Class
 */
public class OutputStreamL1 extends OutputStream {
    private static final int[] controls = {'\u0018', '\u0017'};
    private final EscapedOutputStream escaped;
    private final OutputStream out;
    private final Logger log;
    private LinkedList<ExceptionRunnable> queue;
    private byte[][][][] blocks;
    private int gbL, mbL, kbL, bL;
    private int gb, mb, kb, b;
    private long stored;
    private long limit;

    static {
        Arrays.sort(controls);
    }

    /**
     * Creates a SubData Layer 1 OutputStream
     *
     * @param stream Super stream
     */
    public OutputStreamL1(Logger log, OutputStream stream, long limit) {
        this.log = log;
        this.out = stream;
        this.escaped = new EscapedOutputStream(out, '\u0010', '\u0017');
        this.limit = limit;

        gbL = (int) Math.floor((double) limit / GB);
        mbL = (gbL > 0) ? 1024 : (int) Math.floor((double) limit / MB);
        kbL = (mbL > 0) ? 1024 : (int) Math.floor((double) limit / KB);
        bL = (kbL > 0) ? 1024 : (int) Math.floor((double) limit / 4);

        if (gbL != 1) while (gbL % 4 != 0) ++gbL;
        if (mbL != 1) while (mbL % 4 != 0) ++mbL;
        if (kbL != 1) while (kbL % 4 != 0) ++kbL;
        while (bL % 4 != 0) ++bL;

        if (gbL == 0) gbL = 1;
        if (mbL == 0) mbL = 1;
        if (kbL == 0) kbL = 1;
        if (bL == 0) bL = 1;

        blocks = new byte[gbL][][][];
        blocks[0] = new byte[mbL][][];
        blocks[0][0] = new byte[kbL][];
        blocks[0][0][0] = new byte[bL];
    }

    /**
     * Write data to the stream
     *
     * @param b Data
     * @throws IOException
     */
    @Override
    public void write(int b) throws IOException {
        blocks[gb][mb][kb][this.b++] = (byte) b;
        ++stored;

        if (this.b >= 1024) {
            this.b = 0;
            ++kb;
            if (kb >= 1024) {
                kb = 0;
                ++mb;
                if (mb >= 1024) {
                    mb = 0;
                    ++gb;
                    if (gb >= 1024 || stored >= limit) {
                        flush();
                    }
                    blocks[gb] = new byte[gbL][][];
                } else if (stored >= limit) {
                    flush();
                }
                blocks[gb][mb] = new byte[kbL][];
            }
            blocks[gb][mb][kb] = new byte[bL];
        }
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

        if (this.b != 0 || kb != 0 || mb != 0 || gb != 0) {
            flush();
        }
        queue(() -> {
            out.write(b);
            out.flush();
        });
    }

    /**
     * Flush the stream
     *
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {
        queue(new BlockFlusher(blocks, stored)::flush);

        this.blocks = new byte[gbL][][][];
        this.blocks[0] = new byte[mbL][][];
        this.blocks[0][0] = new byte[kbL][];
        this.blocks[0][0][0] = new byte[bL];
        this.b = 0;
        this.kb = 0;
        this.mb = 0;
        this.gb = 0;
        this.stored = 0;
        this.out.flush();
    }

    private synchronized void queue(ExceptionRunnable runnable) {
        boolean init = false;
        if (queue == null) {
            queue = new LinkedList<>();
            init = true;
        }
        queue.add(runnable);
        if (init) run();
    }
    private void run() {
        if (queue != null) new Thread(() -> {
            if (queue.size() > 0) {
                try {
                    ExceptionRunnable next = Util.getDespiteException(() -> queue.get(0), null);
                    Util.isException(() -> queue.remove(0));
                    if (next != null) {
                        next.run();
                        if (queue != null && queue.size() > 0) run();
                        else queue = null;
                    }
                } catch (Throwable e) {
                    Util.isException(() -> queue.remove(0));
                    if (!(e instanceof SocketException)) { // Cut the write session short when socket issues occur
                        DebugUtil.logException(e, log);

                        if (queue != null && queue.size() > 0) run();
                        else queue = null;
                    } else queue = null;
                }
            } else queue = null;
        }, "SubDataServer::BlockWriter(" + hashCode() + ')').start();
    }
    private final class BlockFlusher {
        private final byte[][][][] blocks;
        private long stored;
        private int gb, mb, kb, b;

        private BlockFlusher(byte[][][][] blocks, long stored) {
            this.blocks = blocks;
            this.stored = stored;
        }

        private void flush() throws IOException {
            while (stored > 0) {
                if (stored >= GBB) {
                    stored -= flushGB();
                } else if (stored >= MBB) {
                    stored -= flushMB();
                } else if (stored >= KBB) {
                    stored -= flushKB();
                } else if (stored >= BB) {
                    stored -= flushRaw();
                } else {
                    stored -= flushEscaped();
                }
            }
        }

        private long flushGB() throws IOException {
            int size = (int) Math.floor((double) stored / GBB);
            if (size > 256) size = 256;
            if (size != 0) {
                out.write('\u0014');
                out.write((byte) (size - 1));
                int limit = (size * 4) + gb;
                for (; gb < limit; ++gb) {
                    if (blocks[gb] == null) break;
                    for (byte[][] mb : blocks[gb]) {
                        if (mb == null) break;
                        for (byte[] kb : mb) {
                            if (kb == null) break;
                            out.write(kb);
                        }
                    }
                }
                return size * GBB;
            } else return 0;
        }
        private long flushMB() throws IOException {
            if (blocks[gb] != null) {
                int size = (int) Math.floor((double) stored / MBB);
                if (size > 256) size = 256;
                if (size != 0) {
                    out.write('\u0013');
                    out.write((byte) (size - 1));
                    int limit = (size * 4) + mb;
                    for (; mb < limit; ++mb) {
                        if (blocks[gb][mb] == null) break;
                        for (byte[] kb : blocks[gb][mb]) {
                            if (kb == null) break;
                            out.write(kb);
                        }
                    }


                    if (mb >= 1024) {
                        mb = 0;
                        ++gb;
                    }
                    return size * MBB;
                } else return 0;
            } else return 0;
        }
        private long flushKB() throws IOException {
            if (blocks[gb] != null && blocks[gb][mb] != null) {
                int size = (int) Math.floor((double) stored / KBB);
                if (size > 256) size = 256;
                if (size != 0) {
                    out.write('\u0012');
                    out.write((byte) (size - 1));
                    int limit = (size * 4) + kb;
                    for (; kb < limit; ++kb) {
                        if (blocks[gb][mb][kb] == null) break;
                        out.write(blocks[gb][mb][kb]);
                    }


                    if (kb >= 1024) {
                        kb = 0;
                        ++mb;
                        if (mb >= 1024) {
                            mb = 0;
                            ++gb;
                        }
                    }
                    return size * KBB;
                } else return 0;
            } else return 0;
        }
        private long flushRaw() throws IOException {
            if (blocks[gb] != null && blocks[gb][mb] != null && blocks[gb][mb][kb] != null) {
                int size = (int) Math.floor((double) stored / BB);
                if (size > 256) size = 256;
                if (size != 0) {
                    out.write('\u0011');
                    out.write((byte) (size - 1));
                    int limit = (size * 4) + b;
                    for (; b < limit; ++b) {
                        out.write(blocks[gb][mb][kb][b]);
                    }


                    if (b >= 1024) {
                        b = 0;
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
                    return size * BB;
                } else return 0;
            } else return 0;
        }
        private long flushEscaped() throws IOException {
            if (blocks[gb] != null && blocks[gb][mb] != null && blocks[gb][mb][kb] != null) {
                int size = (int) (stored % BB);
                if (size != 0) {
                    out.write('\u0010');
                    int i = size + b;
                    for (; b < i; ++b) {
                        escaped.write(blocks[gb][mb][kb][b]);
                    }
                    escaped.control('\u0017');


                    if (b >= 1024) {
                        b = 0;
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
                    return size;
                } else return 0;
            } else return 0;
        }
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