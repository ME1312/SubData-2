package net.ME1312.SubData.Server.Library;

/**
 * Unsigned Number Handler Class
 */
public class UnsignedDataHandler {

    /**
     * To Unsigned Bytes
     *
     * @param number Number
     * @return Byte Array
     */
    public static byte[] toUnsigned(long number) {
        if (number < 0) throw new IllegalArgumentException("Unsigned numbers may not be less than zero: " + number);
        return toUnsigned(number, (int) Math.ceil(Long.toString(number, 16).length() / 2d));
    }

    /**
     * To Unsigned Bytes
     *
     * @param number Number
     * @param length Array Length
     * @return Byte Array
     */
    public static byte[] toUnsigned(long number, int length) {
        if (number < 0) throw new IllegalArgumentException("Unsigned numbers may not be less than zero: " + number);
        byte[] unsigned = new byte[length];
        for (int i = length; i > 0; i--) {
            unsigned[i - 1] = (byte) (number >>> ((length - i) * 8));
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
            signed += bytes[i - 1] << ((bytes.length - i) * 8);
        }
        return signed;
    }
}
