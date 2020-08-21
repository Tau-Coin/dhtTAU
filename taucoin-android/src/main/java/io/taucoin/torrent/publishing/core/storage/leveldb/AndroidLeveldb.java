package io.taucoin.torrent.publishing.core.storage.leveldb;

import io.taucoin.db.KeyValueDataBase;
import io.taucoin.util.ByteUtil;

import com.github.hf.leveldb.exception.LevelDBException;
import com.github.hf.leveldb.LevelDB;
import com.github.hf.leveldb.Iterator;
import com.github.hf.leveldb.util.SimpleWriteBatch;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class AndroidLeveldb implements KeyValueDataBase {

    // leveldb instance
    private LevelDB db;

    // leveldb not opened exception
    private LevelDBException notOpenException;

    /**
     * AndroidLeveldb constructor.
     */
    public AndroidLeveldb() {
        this.db = null;
        this.notOpenException = new LevelDBException("Leveldb hasn't been opened.");
    }

    /**
     * Open kv database.
     *
     * @param path database path which can be accessed
     * @throws Exception
     */
    public void open(String path) throws Exception {

        try {
            this.db = LevelDB.open(path);
        } catch (LevelDBException e) {
            e.printStackTrace();
            LevelDB.repair(path);
            this.db = LevelDB.open(path);
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
        db.put(key, value, true);
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
        db.del(key, true);
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

        SimpleWriteBatch batch = new SimpleWriteBatch(this.db);

        for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
            batch = batch.put(entry.getKey(), entry.getValue());
        }

        // sync write batch
        db.write(batch, true);
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

        SimpleWriteBatch batch = new SimpleWriteBatch(this.db);

        for (Map.Entry<byte[], byte[]> entry : writes.entrySet()) {
            batch = batch.put(entry.getKey(), entry.getValue());
        }

        for (byte[] key : delKeys) {
            batch = batch.del(key);
        }

        // sync write batch
        db.write(batch, true);
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
        Iterator iterator = db.iterator();
        byte[] key = null;

        for (iterator.seek(prefix); iterator.isValid(); iterator.next()) {
            key = iterator.key();
            if (key != null && ByteUtil.startsWith(key, prefix)) {
                results.add(key);
            } else {
                break;
            }
        }
        iterator.close();

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

        Iterator iterator = db.iterator();
        byte[] key = null;

        iterator.seek(prefix);
        for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
            key = iterator.key();
            if (key != null && ByteUtil.startsWith(key, prefix)) {
                db.del(key, true);
            } else {
                break;
            }
        }
        iterator.close();
    }
}
