package net.ME1312.SubData.Client.Library;

/**
 * Unsigned Number Handler Class
 */
public class UnsignedDataHandler {

    /**
     * To Unsigned Bytes
     *
     * @param number Number
     * @param length Array Length
     * @return Byte Array
     */
    public static byte[] toUnsigned(long number, int length) {
        if (number < 0) throw new IllegalArgumentException("Unsigned numbers may not be less than zero: " + number);
        if (length <= 0) length = 1;
        byte[] unsigned = new byte[length];
        for (int i = length; i > 0; i--) {
            unsigned[i - 1] = (byte) ((number >> ((length - i) * 8)) & 0xFF);
        }
        return unsigned;
    }

    /**
     * To Number
     *
     * @param bytes Byte Array
     * @return Number
     */
    public static long fromUnsigned(byte... bytes) {
        long signed = 0;
        for (int i = bytes.length; i > 0; i--) {
            signed += (bytes[i - 1] & 0xFF) << ((bytes.length - i) * 8);
        }
        return signed;
    }
}
