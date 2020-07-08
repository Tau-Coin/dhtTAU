package io.taucoin.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;

public class MemoryPoolPolicy implements Comparator<MemoryPoolEntry> {

    private static final Logger log = LoggerFactory.getLogger(MemoryPoolPolicy.class);

    @Override
    public int compare(MemoryPoolEntry entry1, MemoryPoolEntry entry2) {
        if (Arrays.equals(entry1.txid, entry2.txid)) {
            return 0;
        }

        if (entry1.fee > entry2.fee ) {
            return 1;
        }

        return -1;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }
}
