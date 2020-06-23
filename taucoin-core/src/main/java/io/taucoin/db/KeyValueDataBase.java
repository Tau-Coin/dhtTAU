package io.taucoin.db;

import java.util.Map;
import java.util.Set;

public interface KeyValueDataBase {

    /**
     * Open kv database.
     *
     * @param path database path which can be accessed
     * @throws Exception
     */
    void open(String path) throws Exception;

    /**
     * Close database.
     */
    void close();

    /**
     * Retrieves value by key from the database.
     *
     * @param key
     * @return value
     * @throws Exception
     */
    byte[] get(byte[] key) throws Exception;

    /**
     * Store the key and value into the database.
     *
     * @param key
     * @param value
     * @throws Exception
     */
    void put(byte[] key, byte[] value) throws Exception;

    /**
     * Delete the pair of key and value from the database.
     *
     * @param key
     * @throws Exception
     */
    void delete(byte[] key) throws Exception;

    /**
     * Write batch into the database.
     *
     * @param rows key-value batch
     * @throws Exception
     */
    void updateBatch(Map<byte[], byte[]> rows) throws Exception;

    /**
     * Retrieves keys with prefix.
     *
     * @param prefix
     * @return Set<byte[]> the keys with the prefix
     * @throws Exception
     */
    Set<byte[]> retrieveKeysWithPrefix(byte[] prefix) throws Exception;

    /**
     * Delete records with the key with the prefix.
     *
     * @param prefix
     * @throws Exception
     */
    void removeWithKeyPrefix(byte[] prefix) throws Exception;
}
