package io.taucoin.torrent.publishing.core.storage.leveldb;

import io.taucoin.db.KeyValueDataBase;
import io.taucoin.db.KeyValueDataBaseFactory;

public class AndroidLeveldbFactory implements KeyValueDataBaseFactory {

    /**
     * AndroidLeveldbFactory constructor.
     */
    public AndroidLeveldbFactory() {}

    /**
     * Create new key value database.
     *
     * @return KeyValueDataBase
     */
    public KeyValueDataBase newDatabase() {
        return new AndroidLeveldb();
    }
}
