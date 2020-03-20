package net.ME1312.SubData.Client.Library;

/**
 * SubData Block Size Enum
 */
public class DataSize {
    public static final byte   B  =      1;
    public static final byte   BB =      4;
    public static final short KB  =      1024;
    public static final short KBB = KB * 4;
    public static final int   MB  = KB * 1024;
    public static final int   MBB = MB * 4;
    public static final int   GB  = MB * 1024;
    public static final long  GBB = GB * 4L;
    public static final long  TB  = GB * 1024L;
}
