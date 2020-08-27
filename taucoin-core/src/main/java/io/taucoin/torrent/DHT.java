package io.taucoin.torrent;

import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.Sha1Hash;

import java.nio.charset.Charset;

/**
 * This class defines some structures about getting and putting immutable and mutable items.
 */

public final class DHT {

    public static class ImmutableItem {
    
        Entry entry;

        public ImmutableItem(byte[] data) {
            this.entry = Entry.bdecode(data);
            // this.entry = new Entry(new String(data, Charset.forName("UTF-8")));
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
            // this.entry = new Entry(new String(data, Charset.forName("UTF-8")));
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
        Sha1Hash putSha1hash;

        public ExchangeImmutableItemResult(byte[] getData, Sha1Hash putSha1hash) {
            this.getData = getData;
            this.putSha1hash = putSha1hash;
        }

        public byte[] getData() {
            return this.getData;
        }

        public Sha1Hash getSha1hash() {
            return this.putSha1hash;
        }
    }

    public static class ExchangeMutableItemResult {
    
        byte[] getData;

        public ExchangeMutableItemResult(byte[] getData) {
            this.getData = getData;
        }
    }

    public static interface GetDHTItemCallback {

        /*
         * This method is called back when dht item(mutable or immutable) got.
         * Note: the implementation of this method must not block current thread.
         */
        void onDHTItemGot(byte[] item, Object cbData);
    }

    /**
     * The wrapper of getting immutable request.
     */
    public static class ImmutableItemRequest {

        private GetImmutableItemSpec spec;

        private GetDHTItemCallback callback;

        private Object callBackData;

        public ImmutableItemRequest(GetImmutableItemSpec spec,
                GetDHTItemCallback callback, Object cbData) {

            this.spec = spec;
            this.callback = callback;
            this.callBackData = cbData;
        }

        public GetImmutableItemSpec getSpec() {
            return this.spec;
        }

        public void onDHTItemGot(byte[] item) {
            if (callback != null) {
                callback.onDHTItemGot(item, this.callBackData);
            }
        }
    }

    /**
     * The wrapper of getting mutable request.
     */
    public static class MutableItemRequest {

        private GetMutableItemSpec spec;

        private GetDHTItemCallback callback;

        private Object callBackData;

        public MutableItemRequest(GetMutableItemSpec spec,
                GetDHTItemCallback callback, Object cbData) {

            this.spec = spec;
            this.callback = callback;
            this.callBackData = cbData;
        }

        public GetMutableItemSpec getSpec() {
            return this.spec;
        }

        public void onDHTItemGot(byte[] item) {
            if (callback != null) {
                callback.onDHTItemGot(item, this.callBackData);
            }
        }
    }
}
