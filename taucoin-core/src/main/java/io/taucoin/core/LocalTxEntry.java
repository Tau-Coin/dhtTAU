package io.taucoin.core;

import io.taucoin.types.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class LocalTxEntry {

    private static final Logger log = LoggerFactory.getLogger("LocalTxEntry");

    public byte[] txid;
    public long timestamp;
    public long nonce;

    private LocalTxEntry() {}

    public LocalTxEntry(byte[] txid, long timestamp, long nonce) {
        this.txid = txid;
        this.timestamp = timestamp;
        this.nonce = nonce;
    }

    public static LocalTxEntry with(Transaction tx) {
        return new LocalTxEntry(tx.getTxID(), tx.getTimeStamp(), tx.getNonce());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalTxEntry that = (LocalTxEntry) o;

        return Arrays.equals(txid, that.txid);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(txid);
    }
}
