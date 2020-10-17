package io.taucoin.core;

import io.taucoin.types.Transaction;
import io.taucoin.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

public class MemoryPoolEntry {

    private static final Logger logger = LoggerFactory.getLogger(MemoryPoolEntry.class);

    public byte[] txid;
    public BigInteger fee;

    public MemoryPoolEntry(byte[] txid, BigInteger fee) {
        this.txid = txid;
        this.fee = fee;
    }

    public static MemoryPoolEntry with(Transaction tx) {
        return new MemoryPoolEntry(tx.getTxID(), tx.getTxFee());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MemoryPoolEntry that = (MemoryPoolEntry) o;

        return Arrays.equals(txid, that.txid);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(txid);
    }

    @Override
    public String toString() {
        return "MemoryPoolEntry{" +
                "txid=" + Hex.toHexString(txid) +
                ", fee=" + fee +
                '}';
    }
}
