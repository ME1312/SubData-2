package net.ME1312.SubData.Server.Library;

/**
 * Ping Response Class<br>
 * All values presented by this class have been recorded in nanoseconds
 */
public class PingResponse {
    private final long qL, qR, t1, t2, r;

    /**
     * Calculates and stores the meaning of a ping using its recorded timings
     *
     * @param timings Ping Timings
     */
    public PingResponse(long[] timings) {
        qL = timings[1] - timings[0];
        qR = timings[3] - timings[2];
        t1 = timings[2] - timings[1];
        t2 = timings[4] - timings[3];
        r  = timings[4] - timings[0];
    }

    /**
     * Get how long the ping waited in a queue on this machine
     *
     * @return Local Queue Time
     */
    public long getLocalQueueTime() {
        return qL;
    }

    /**
     * Get how long the ping waited in a queue on the remote machine
     *
     * @return Remote Queue Time
     */
    public long getRemoteQueueTime() {
        return qR;
    }

    /**
     * Get how long the ping waited in a queue
     *
     * @return Queue Time
     */
    public long getQueueTime() {
        return qL + qR;
    }

    /**
     * @deprecated Get how long it took to upload the ping
     * @see System#nanoTime() This method is wildly inaccurate in real-world scenarios. It may only be valid for evaluating a connection to-and-from the same JVM instance.
     * See System.nanoTime() for more information regarding these limitations.
     *
     * @return Upload Transfer Time
     */
    @Deprecated
    public long getUploadTransferTime() {
        return t1;
    }

    /**
     * @deprecated Get how long it took to download the ping response
     * @see System#nanoTime() This method is wildly inaccurate in real-world scenarios. It may only be valid for evaluating a connection to-and-from the same JVM instance.
     * See System.nanoTime() for more information regarding these limitations.
     *
     * @return Download Transfer Time
     */
    @Deprecated
    public long getDownloadTransferTime() {
        return t2;
    }

    /**
     * Get how long it took to transfer the ping over the network
     *
     * @return Transfer Time
     */
    public long getTransferTime() {
        return t1 + t2; // These two wildly inaccurate fields cancel each-other out when added, creating a perfectly reliable transfer metric.
    }

    /**
     * Get how long it took to receive the ping response in total
     *
     * @return Response Time
     */
    public long getResponseTime() {
        return r;
    }
}
