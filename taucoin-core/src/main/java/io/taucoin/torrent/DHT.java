package io.taucoin.torrent;

import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.Sha1Hash;

/**
 * This class defines some structures about getting and putting immutable and mutable items.
 */

public final class DHT {

    public static class ImmutableItem {
    
        Entry entry;

        public ImmutableItem(byte[] data) {
            this.entry = Entry.bdecode(data);
        }
    }

    public static class MutableItem {

        byte[] publicKey;
        byte[] privateKey;
        Entry entry;
        byte[] salt;

        public MutableItem(byte[] publicKey, byte[] privateKey, byte[] data, byte[] salt) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.entry = Entry.bdecode(data);
            this.salt= salt;
        }
    }

    public static class GetImmutableItemSpec {

        Sha1Hash sha1;
        int timeout;

        public GetImmutableItemSpec(byte[] sha1, int timeout) {
            this.sha1 = new Sha1Hash(sha1);
            this.timeout = timeout;
        }
    }

    public static class GetMutableItemSpec {
    
        byte[] publicKey;
        byte[] salt;
        int timeout;

        public GetMutableItemSpec(byte[] publicKey, byte[] salt, int timeout) {
            this.publicKey = publicKey;
            this.salt = salt;
            this.timeout = timeout;
        }
    }

    public static class ExchangeImmutableItemResult {
    
        byte[] getData;
        byte[] putSha1hash;

        public ExchangeImmutableItemResult(byte[] getData, byte[] putSha1hash) {
            this.getData = getData;
            this.putSha1hash = putSha1hash;
        }
    }

    public static class ExchangeMutableItemResult {
    
        byte[] getData;

        public ExchangeMutableItemResult(byte[] getData) {
            this.getData = getData;
        }
    }
}
