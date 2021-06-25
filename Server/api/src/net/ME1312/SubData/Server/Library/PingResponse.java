package net.ME1312.SubData.Server.Library;

/**
 * Ping Response Class
 */
public class PingResponse {
    private final long qL, qR, t1, t2;

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
     * Get how long it took to transfer the ping over the network
     *
     * @return Transfer Time
     */
    public long getTransferTime() {
        return t1 + t2;
    }

    /**
     * Get how long it took to upload the ping
     *
     * @return Upload Transfer Time
     */
    public long getUploadTransferTime() {
        return t1;
    }

    /**
     * Get how long it took to download the ping response
     *
     * @return Download Transfer Time
     */
    public long getDownloadTransferTime() {
        return t2;
    }

    /**
     * Get how long it took to receive the ping response in total
     *
     * @return Response Time
     */
    public long getResponseTime() {
        return qL + qR + t1 + t2;
    }
}
