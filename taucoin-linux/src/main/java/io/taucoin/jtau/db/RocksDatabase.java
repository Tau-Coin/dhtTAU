package io.taucoin.jtau.db;

import io.taucoin.db.KeyValueDataBase;
import io.taucoin.util.ByteUtil;

import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Options;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * RocksDatabase implements key-value database through facebook rocdsdb.
 */
public class RocksDatabase implements KeyValueDataBase {

    private static final Logger logger = LoggerFactory.getLogger("rocksdb");

    // rocksdb instance
    private RocksDB db;

    // rocksdb not opened exception
    private RocksDBException notOpenException;

    // sync write option
    private final WriteOptions syncWriteOptions;

    static {
        RocksDB.loadLibrary();
    }

    /**
     * RocksDatabase constructor.
     */
    public RocksDatabase() {
        this.db = null;
        this.notOpenException = new RocksDBException("Rocksdb hasn't been opened.");

        this.syncWriteOptions = new WriteOptions();
        this.syncWriteOptions.setSync(true);
    }

    /**
     * Open kv database.
     *
     * @param path database path which can be accessed
     * @throws Exception
     */
    public void open(String path) throws Exception {

        try {
            this.db = RocksDB.open(path);
        } catch (RocksDBException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Close database.
     */
    public void close() {
        if (db == null) {
            return;
        }
        db.close();
    }

    /**
     * Retrieves value by key from the database.
     *
     * @param key
     * @return value
     * @throws Exception
     */
    public byte[] get(byte[] key) throws Exception {
        if (db == null) {
            throw notOpenException;
        }

        return db.get(key);
    }

    /**
     * Store the key and value into the database.
     *
     * @param key
     * @param value
     * @throws Exception
     */
    public void put(byte[] key, byte[] value) throws Exception {
        if (db == null) {
            throw notOpenException;
        }

        // sync put
        db.put(syncWriteOptions, key, value);
    }

    /**
     * Delete the pair of key and value from the database.
     *
     * @param key
     * @throws Exception
     */
    public void delete(byte[] key) throws Exception {
        if (db == null) {
            throw notOpenException;
        }

        // sync delete
        db.remove(syncWriteOptions, key);
    }

   /**
     * Write batch into the database.
     *
     * @param rows key-value batch
     * @throws Exception
     */
    public void updateBatch(Map<byte[], byte[]> rows) throws Exception {
        if (db == null) {
            throw notOpenException;
        }

        WriteBatch batch = new WriteBatch();

        for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
            batch.put(entry.getKey(), entry.getValue());
        }

        // sync write batch
        db.write(syncWriteOptions, batch);
    }

    /**
     * Write batch(write and delete) into the database.
     * @param writes
     * @param delKeys
     * @throws Exception
     */
     public void updateBatch(Map<byte[], byte[]> writes, Set<byte[]> delKeys) throws Exception {
        if (db == null) {
            throw notOpenException;
        }

        WriteBatch batch = new WriteBatch();

        for (Map.Entry<byte[], byte[]> entry : writes.entrySet()) {
            batch.put(entry.getKey(), entry.getValue());
        }

        for (byte[] key : delKeys) {
            batch.remove(key);
        }

        // sync write batch
        db.write(syncWriteOptions, batch);
    }

    /**
     * Retrieves keys with prefix.
     *
     * @param prefix
     * @return Set<byte[]> the keys with the prefix
     * @throws Exception
     */
    public Set<byte[]> retrieveKeysWithPrefix(byte[] prefix) throws Exception {
        if (db == null) {
            throw notOpenException;
        }

        Set<byte[]> results = new HashSet<byte[]>();
        RocksIterator iterator = db.newIterator();
        byte[] key = null;

        logger.debug("retrieveKeysWithPrefix prefix str:" + new String(prefix)
                 + "hex:" + Hex.toHexString(prefix));

        for (iterator.seek(prefix); iterator.isValid(); iterator.next()) {
            key = iterator.key();
            logger.debug("iterator key:" + (key != null ? Hex.toHexString(key) : "null"));
            if (key != null && ByteUtil.startsWith(key, prefix)) {
                results.add(key);
            } else {
                break;
            }
        }

        logger.debug("retrieveKeysWithPrefix result size:" + results.size());
        return results;
    }

    /**
     * Delete records with the key with the prefix.
     *
     * @param prefix
     * @throws Exception
     */
    public void removeWithKeyPrefix(byte[] prefix) throws Exception {
        if (db == null) {
            throw notOpenException;
        }

        RocksIterator iterator = db.newIterator();
        byte[] key = null;

        iterator.seek(prefix);
        for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
            key = iterator.key();
            if (key != null && ByteUtil.startsWith(key, prefix)) {
                db.remove(syncWriteOptions, key);
            } else {
                break;
            }
        }
    }
}
