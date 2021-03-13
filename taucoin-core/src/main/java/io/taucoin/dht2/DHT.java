package io.taucoin.dht2;

import io.taucoin.dht2.util.Utils;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.FastByteComparisons;
import io.taucoin.util.HashUtil;

import com.frostwire.jlibtorrent.Entry;
import com.frostwire.jlibtorrent.Sha1Hash;
import com.frostwire.jlibtorrent.swig.byte_vector;
import com.frostwire.jlibtorrent.swig.sha1_hash;
import com.frostwire.jlibtorrent.Vectors;
import org.spongycastle.util.encoders.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ArrayList;

/**
 * This class defines some structures about getting and putting immutable and mutable items.
 */

public final class DHT {

    // Timeout value for getting immutable & mutable item.
    public static final int DHT_OP_TIMEOUT = 10;

    public static class ImmutableItem {
    
        public Entry entry;
        byte[] entryBytes;

        private Sha1Hash hash;

        public ImmutableItem(byte[] data) {
            this.entryBytes = data;
            this.entry = Utils.fromStringBytes(data);
            this.hash = computeHash();
        }

        public Sha1Hash hash() {
            return hash;
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
            return hash.hashCode();
        }

        @Override
        public String toString() {
            if (entry == null) {
                return "";
            }

            return hash.toString();
        }

        private Sha1Hash computeHash() {
            return new Sha1Hash(HashUtil.sha1hash(entry.bencode()));
        }
     }

    public static class MutableItem {

        public byte[] publicKey;
        public byte[] privateKey;
        public Entry entry;
        byte[] entryBytes;
        public byte[] salt;

        private Sha1Hash hash;

        public MutableItem(byte[] publicKey, byte[] privateKey, byte[] data, byte[] salt) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.entryBytes = data;
            this.entry = Utils.fromStringBytes(data);
            this.salt = salt;
            this.hash = computeHash(this.publicKey, this.salt);
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
            return hash.swig().hash_code();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("MutableItem(");
            sb.append("pubkey:" + Hex.toHexString(publicKey));
            sb.append(",salt:" + new String(salt));
            //sb.append(",e:" + entry.toString());
            sb.append(")");

            return sb.toString();
        }

        public Sha1Hash hash() {
            return hash;
        }

        public static Sha1Hash computeHash(byte[] publicKey, byte[] salt) {
            MessageDigest digest = null;

            try {
                digest = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                // For sha1 this exception should never happens.
                return null;
            }

            digest.update(publicKey);
            //digest.update(privateKey);
            digest.update(salt);
            //digest.update(entryBytes);

            byte_vector bvs = Vectors.bytes2byte_vector(digest.digest());
            sha1_hash hash = new sha1_hash(bvs);

            return new Sha1Hash(hash);
        }
    }

    // Put many mutable items in one time call to forstwire 'dhtput'.
    public static class MutableItemBatch {

        public byte[] publicKey;
        public byte[] privateKey;
        public List<Entry> entryList;
        public byte[] salt;

        private Sha1Hash hash;

        public MutableItemBatch(byte[] publicKey, byte[] privateKey,
                List<ByteArrayWrapper> items, byte[] salt) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.salt = salt;
            this.hash = MutableItem.computeHash(this.publicKey, this.salt);
            // construct entry list
            this.entryList = fromBytesArray(items);
        }

        public boolean isValid() {
            if (publicKey == null || privateKey == null || salt == null
                    || entryList == null || entryList.isEmpty()) {
                return false;
            }

            return true;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MutableItemBatch)) {
                return false;
            }

            MutableItemBatch items = (MutableItemBatch)obj;

            return hashCode() == items.hashCode();
        }

        @Override
        public int hashCode() {
            return hash.swig().hash_code();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("MutableItemBatch(");
            sb.append("pubkey:" + Hex.toHexString(publicKey));
            sb.append(", salt:" + new String(salt));
            sb.append(")");

            return sb.toString();
        }

        public Sha1Hash hash() {
            return hash;
        }

        public static List<Entry> fromBytesArray(List<ByteArrayWrapper> items) {
            List<Entry> entryList = new ArrayList<Entry>();

            for (ByteArrayWrapper i : items) {
                entryList.add(Utils.fromStringBytes(i.getData()));
            }

            return entryList;
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

        public Sha1Hash hash() {
            return sha1;
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

        private Sha1Hash hash;

        public GetMutableItemSpec(byte[] publicKey, byte[] salt, int timeout) {
            this.publicKey = publicKey;
            this.salt = salt;
            this.timeout = timeout;

            this.hash = MutableItem.computeHash(publicKey, salt);
        }

        public GetMutableItemSpec(byte[] publicKey, byte[] salt) {
            this(publicKey, salt, DHT_OP_TIMEOUT);
        }

        public Sha1Hash hash() {
            return hash;
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
            return hash.hashCode();
        }

        @Override
        public String toString() {
            return "(" + "pubkey:" + Hex.toHexString(publicKey)
                + ", salt:" + new String(salt) + ")";
        }
    }

    public static interface GetImmutableItemCallback {

        /*
         * This method is called back when dht immutable item got.
         * Note: the implementation of this method must not block current thread.
         */
        void onDHTItemGot(byte[] item, Object cbData);
    }

    public static interface GetMutableItemCallback {

        /*
         * This method is called back when dht mutable item got.
         * Note: the implementation of this method must not block current thread.
         */
        void onDHTItemGot(byte[] item, Object cbData, boolean auth);
    }

    public static interface PutDHTItemCallback {

        /*
         * This method is called back when dht item(mutable or immutable) is put.
         * Note: the implementation of this method must not block current thread.
         */
        void onDHTItemPut(int success, Object cbData);
    }

    /**
     * The wrapper of getting immutable request.
     */
    public static class ImmutableItemRequest extends Trace {

        public GetImmutableItemSpec spec;

        private GetImmutableItemCallback callback;

        private Object callBackData;

        public ImmutableItemRequest(GetImmutableItemSpec spec,
                GetImmutableItemCallback callback, Object cbData) {

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

        public GetImmutableItemCallback getCallback() {
            return this.callback;
        }

        public void onDHTItemGot(byte[] item) {
            if (callback != null) {
                callback.onDHTItemGot(item, this.callBackData);
            }
        }

        public Sha1Hash hash() {
            return spec.hash();
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
    public static class MutableItemRequest extends Trace {

        public GetMutableItemSpec spec;

        private GetMutableItemCallback callback;

        private Object callBackData;

        public MutableItemRequest(GetMutableItemSpec spec,
                GetMutableItemCallback callback, Object cbData) {

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

        public GetMutableItemCallback getCallback() {
            return this.callback;
        }

        public void onDHTItemGot(byte[] item, boolean auth) {
            if (callback != null) {
                callback.onDHTItemGot(item, this.callBackData, auth);
            }
        }

        public Sha1Hash hash() {
            return spec.hash();
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

    /**
     * The wrapper of immutable item distribution.
     */
    public static class ImmutableItemDistribution extends Trace {

        public ImmutableItem item;

        public PutDHTItemCallback callback;

        private Object callBackData;

        public ImmutableItemDistribution(ImmutableItem item,
                PutDHTItemCallback callback, Object cbData) {
            this.item = item;
            this.callback = callback;
            this.callBackData = cbData;
        }

        public ImmutableItem getItem() {
            return this.item;
        }

        public Object getCallbackData() {
            return this.callBackData;
        }

        public PutDHTItemCallback getCallback() {
            return this.callback;
        }

        public Sha1Hash hash() {
            return this.item.hash();
        }

        public void onDHTItemPut(int success) {
            if (callback != null) {
                callback.onDHTItemPut(success, this.callBackData);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ImmutableItemDistribution)) {
                return false;
            }

            ImmutableItemDistribution d = (ImmutableItemDistribution)obj;

            return item.equals(d.item);
        }

        @Override
        public int hashCode() {
            return item.hashCode();
        }

        @Override
        public String toString() {
            return item.toString();
        }
    }

    /**
     * The wrapper of mutable item distribution.
     */
    public static class MutableItemDistribution extends Trace {

        public MutableItem item;

        public PutDHTItemCallback callback;

        private Object callBackData;

        public MutableItemDistribution(MutableItem item,
                PutDHTItemCallback callback, Object cbData) {
            this.item = item;
            this.callback = callback;
            this.callBackData = cbData;
        }

        public MutableItem getItem() {
            return this.item;
        }

        public Object getCallbackData() {
            return this.callBackData;
        }

        public PutDHTItemCallback getCallback() {
            return this.callback;
        }

        public Sha1Hash hash() {
            return this.item.hash();
        }

        public void onDHTItemPut(int success) {
            if (callback != null) {
                callback.onDHTItemPut(success, this.callBackData);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MutableItemDistribution)) {
                return false;
            }

            MutableItemDistribution d = (MutableItemDistribution)obj;

            return item.equals(d.item);
        }

        @Override
        public int hashCode() {
            return item.hashCode();
        }

        @Override
        public String toString() {
            return item.toString();
        }
    }

    /**
     * The wrapper of mutable item distribution.
     */
    public static class MutableItemBatchDistribution extends Trace {

        public MutableItemBatch items;

        public PutDHTItemCallback callback;

        private Object callBackData;

        public MutableItemBatchDistribution(MutableItemBatch items,
                PutDHTItemCallback callback, Object cbData) {
            this.items = items;
            this.callback = callback;
            this.callBackData = cbData;
        }

        public MutableItemBatch getItems() {
            return this.items;
        }

        public Object getCallbackData() {
            return this.callBackData;
        }

        public PutDHTItemCallback getCallback() {
            return this.callback;
        }

        public Sha1Hash hash() {
            return this.items.hash();
        }

        public void onDHTItemPut(int success) {
            if (callback != null) {
                callback.onDHTItemPut(success, this.callBackData);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MutableItemDistribution)) {
                return false;
            }

            MutableItemBatchDistribution d = (MutableItemBatchDistribution)obj;

            return items.equals(d.items);
        }

        @Override
        public int hashCode() {
            return items.hashCode();
        }

        @Override
        public String toString() {
            return items.toString();
        }
    }

    public static class Trace {

        protected long start = 0;
        protected long end = 0;

        public Trace() {}

        public void start() {
            this.start = System.nanoTime();
        }

        public long duration() {
            return (System.nanoTime() - this.start);
        }

        public void end() {
            this.end = System.nanoTime();
        }

        public long cost() {
            return (end -start);
        }
    }
}
