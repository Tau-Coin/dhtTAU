package io.taucoin.dht;

import io.taucoin.dht.util.Utils;
import io.taucoin.util.FastByteComparisons;
import io.taucoin.util.HashUtil;

import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.Sha1Hash;
import com.frostwire.jlibtorrent.swig.byte_vector;
import com.frostwire.jlibtorrent.swig.sha1_hash;
import com.frostwire.jlibtorrent.Vectors;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class defines some structures about getting and putting immutable and mutable items.
 */

public final class DHT {

    // Timeout value for getting immutable & mutable item.
    public static final int DHT_OP_TIMEOUT = 10;

    public static class ImmutableItem {
    
        public Entry entry;
        byte[] entryBytes;

        public ImmutableItem(byte[] data) {
            this.entryBytes = data;
            this.entry = Utils.fromStringBytes(data);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ImmutableItem)) {
                return false;
            }

            ImmutableItem item = (ImmutableItem)obj;

            return 0 == FastByteComparisons.compareTo(
                    entryBytes, 0, entryBytes.length,
                    item.entryBytes, 0, item.entryBytes.length);
        }

        @Override
        public int hashCode() {
            Sha1Hash hash = new Sha1Hash(HashUtil.sha1hash(entryBytes));
            return hash.hashCode();
        }

        @Override
        public String toString() {
            if (entry == null) {
                return "";
            }

            return entry.toString();
        }
     }

    public static class MutableItem {

        public byte[] publicKey;
        public byte[] privateKey;
        public Entry entry;
        byte[] entryBytes;
        public byte[] salt;

        public MutableItem(byte[] publicKey, byte[] privateKey, byte[] data, byte[] salt) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.entryBytes = data;
            this.entry = Utils.fromStringBytes(data);
            this.salt = salt;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MutableItem)) {
                return false;
            }

            MutableItem item = (MutableItem)obj;

            return hashCode() == item.hashCode();
        }

        @Override
        public int hashCode() {
            MessageDigest digest = null;

            try {
                digest = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                // For sha1 this exception should never happens.
                return 0;
            }

            digest.update(publicKey);
            digest.update(privateKey);
            digest.update(salt);
            digest.update(entryBytes);

            byte_vector bvs = Vectors.bytes2byte_vector(digest.digest());
            sha1_hash hash = new sha1_hash(bvs);

            return hash.hash_code();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("MutableItem(");
            sb.append("pubkey:" + Hex.toHexString(publicKey));
            sb.append(",salt:" + Hex.toHexString(salt));
            sb.append(",e:" + entry.toString());
            sb.append(")");

            return sb.toString();
        }
    }

    public static class GetImmutableItemSpec {

        public Sha1Hash sha1;
        public int timeout;

        public GetImmutableItemSpec(byte[] sha1, int timeout) {
            this.sha1 = new Sha1Hash(sha1);
            this.timeout = timeout;
        }

        public GetImmutableItemSpec(byte[] sha1) {
            this(sha1, DHT_OP_TIMEOUT);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof GetImmutableItemSpec)) {
                return false;
            }

            GetImmutableItemSpec spec = (GetImmutableItemSpec)obj;

            return sha1.equals(spec.sha1) && (timeout == spec.timeout);
        }

        @Override
        public int hashCode() {
            return sha1.hashCode();
        }

        @Override
        public String toString() {
            return sha1.toString();
        }
    }

    public static class GetMutableItemSpec {
    
        public byte[] publicKey;
        public byte[] salt;
        public int timeout;

        public GetMutableItemSpec(byte[] publicKey, byte[] salt, int timeout) {
            this.publicKey = publicKey;
            this.salt = salt;
            this.timeout = timeout;
        }

        public GetMutableItemSpec(byte[] publicKey, byte[] salt) {
            this(publicKey, salt, DHT_OP_TIMEOUT);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof GetMutableItemSpec)) {
                return false;
            }

            GetMutableItemSpec spec = (GetMutableItemSpec)obj;

            return (0 == FastByteComparisons.compareTo(
                    publicKey, 0, publicKey.length,
                    spec.publicKey, 0, spec.publicKey.length))
                && (0 == FastByteComparisons.compareTo(
                    salt, 0, salt.length,
                    spec.salt, 0, spec.salt.length))
                && (timeout == spec.timeout);
        }

        @Override
        public int hashCode() {
            MessageDigest digest = null;

            try {
                digest = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                // For sha1 this exception should never happens.
                return 0;
            }

            digest.update(publicKey);
            digest.update(salt);

            byte_vector bvs = Vectors.bytes2byte_vector(digest.digest());
            sha1_hash hash = new sha1_hash(bvs);

            return hash.hash_code();
        }

        @Override
        public String toString() {
            return "(" + "pubkey:" + Hex.toHexString(publicKey)
                + ", salt:" + Hex.toHexString(salt) + ")";
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

        public GetImmutableItemSpec spec;

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

        public Object getCallbackData() {
            return this.callBackData;
        }

        public void onDHTItemGot(byte[] item) {
            if (callback != null) {
                callback.onDHTItemGot(item, this.callBackData);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ImmutableItemRequest)) {
                return false;
            }

            ImmutableItemRequest req = (ImmutableItemRequest)obj;

            return spec.equals(req.spec);
        }

        @Override
        public int hashCode() {
            return spec.hashCode();
        }

        @Override
        public String toString() {
            return spec.toString();
        }
    }

    /**
     * The wrapper of getting mutable request.
     */
    public static class MutableItemRequest {

        public GetMutableItemSpec spec;

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

        public Object getCallbackData() {
            return this.callBackData;
        }

        public void onDHTItemGot(byte[] item) {
            if (callback != null) {
                callback.onDHTItemGot(item, this.callBackData);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MutableItemRequest)) {
                return false;
            }

            MutableItemRequest req = (MutableItemRequest)obj;

            return spec.equals(req.spec);
        }

        @Override
        public int hashCode() {
            return spec.hashCode();
        }

        @Override
        public String toString() {
            return spec.toString();
        }
    }
}
