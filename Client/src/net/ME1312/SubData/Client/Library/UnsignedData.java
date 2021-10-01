package net.ME1312.SubData.Client.Library;

/**
 * Unsigned Data Conversion Class
 */
public class UnsignedData {

    /**
     * Convert Signed Number to Unsigned Bytes
     *
     * @param number Number
     * @param length Array Length
     * @return Byte Array
     */
    public static byte[] unsign(long number, int length) {
        if (number < 0) throw new IllegalArgumentException("Unsigned numbers may not be less than zero: " + number);
        if (length <= 0) length = 1;
        byte[] unsigned = new byte[length];
        for (int i = length; i > 0; i--) {
            unsigned[i - 1] = (byte) ((number >> ((length - i) * 8)) & 0xFF);
        }
        return unsigned;
    }

    /**
     * Convert from Unsigned Bytes to Signed Number
     *
     * @param bytes Byte Array
     * @return Number
     */
    public static long resign(byte... bytes) {
        long signed = 0;
        for (int i = bytes.length; i > 0; i--) {
            signed += (long) (bytes[i - 1] & 0xFF) << ((bytes.length - i) * 8);
        }
        return signed;
    }
}
