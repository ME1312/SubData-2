package net.ME1312.SubData.Client.Library;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

/**
 * Debugging Utility Class
 */
public class DebugUtil {

    /**
     * Convert to hex notation (ex. 0x0001)
     *
     * @param example Notation Example (0xFFFF for 16-bit whitespace in this example)
     * @param value Value to Convert (1 in this example)
     * @return Converted Value (0x0001 is the result of this example)
     */
    public static String toHex(int example, int value) {
        boolean negative = value < 0;
        if (negative) value *= -1;
        String ex = Integer.toHexString(example);
        StringBuilder hex = new StringBuilder(Integer.toHexString(value).toUpperCase());
        while (hex.length() < ex.length()) hex.insert(0, "0");
        return ((negative)?"-":"") + "0x" + hex;
    }

    /**
     * Log an exception to a primitive logger
     *
     * @param e Exception
     * @param log Primitive Logger
     */
    public static void logException(Throwable e, Logger log) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log.severe(sw.toString());
    }
}
