package io.taucoin.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;

public class LocalTxPolicy implements Comparator<LocalTxEntry> {

    private static final Logger logger = LoggerFactory.getLogger("LocalTxPolicy");

    @Override
    public int compare(LocalTxEntry entry1, LocalTxEntry entry2) {
        if (Arrays.equals(entry1.txid, entry2.txid)) {
            return 0;
        }

        if (entry1.nonce < entry2.nonce) {
            return -1;
        } else if (entry1.nonce == entry2.nonce) {
            if (entry1.timestamp > entry2.timestamp) {
                return -1;
            }
        }

        return 1;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }
}
