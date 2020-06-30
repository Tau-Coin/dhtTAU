package io.taucoin.db;

/**
 * Factory to produce new key value data base.
 */
public interface KeyValueDataBaseFactory {

    /**
     * Create new key value database.
     *
     * @return KeyValueDataBase
     */
    KeyValueDataBase newDatabase();
}
