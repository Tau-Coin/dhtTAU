package io.taucoin.dht.util;

import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.swig.entry;
import com.frostwire.jlibtorrent.swig.byte_vector;
import com.frostwire.jlibtorrent.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class Utils {

    private static final String sUndefinedEntry = entry.data_type.undefined_t.toString();

    public static String getEntryType(Entry e) {
        if (e == null || e.swig() == null) {
            return "";
        }

        entry eswig = e.swig();
        return eswig.type().toString();
    }

    public static boolean isEntryUndefined(Entry e) {
        return sUndefinedEntry.equals(getEntryType(e));
    }

    public static void printStacktraceToLogger(Logger logger, Throwable t) {
        if (t == null || logger == null) {
            return;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String utf8 = StandardCharsets.UTF_8.name();

        try {
            PrintStream ps = new PrintStream(baos, true, utf8);
            t.printStackTrace(ps);
            logger.error(baos.toString(utf8));
        } catch (Exception e) {}
    }

    public static Entry fromPreformattedBytes(byte[] data) {
        entry e = entry.from_preformatted_bytes(Vectors.bytes2byte_vector(data));
        return new Entry(e);
    }

    public static byte[] preformattedEntryToBytes(Entry item) {
        if (item == null || item.swig().type() != entry.data_type.preformatted_t) {
            return null;
        }

        byte_vector bv = item.swig().preformatted_bytes();
        return Vectors.byte_vector2bytes(bv);
    }

    public static Entry fromStringBytes(byte[] data) {
        entry e = entry.from_string_bytes(Vectors.bytes2byte_vector(data));
        return new Entry(e);
    }

    public static byte[] stringEntryToBytes(Entry item) {
        if (item == null || item.swig().type() != entry.data_type.string_t) {
            return null;
        }

        byte_vector bv = item.swig().string_bytes();
        return Vectors.byte_vector2bytes(bv);
    }
}
