package io.taucoin.core;

import java.math.BigInteger;
import java.util.Arrays;

public class HashPrefixArrayInfo {
    byte[] hashPrefixArray;
    BigInteger timestamp;

    public HashPrefixArrayInfo(byte[] hashPrefixArray, BigInteger timestamp) {
        this.hashPrefixArray = hashPrefixArray;
        this.timestamp = timestamp;
    }

    public byte[] getHashPrefixArray() {
        return hashPrefixArray;
    }

    public BigInteger getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashPrefixArrayInfo that = (HashPrefixArrayInfo) o;
        return Arrays.equals(hashPrefixArray, that.hashPrefixArray);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(hashPrefixArray);
    }
}
