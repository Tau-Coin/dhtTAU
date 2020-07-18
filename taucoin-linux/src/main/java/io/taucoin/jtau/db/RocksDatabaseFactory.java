package io.taucoin.jtau.db;

import io.taucoin.db.KeyValueDataBase;
import io.taucoin.db.KeyValueDataBaseFactory;

public class RocksDatabaseFactory implements KeyValueDataBaseFactory {

    /**
     * RocksDatabaseFactory constructor.
     */
    public RocksDatabaseFactory() {}

    /**
     * Create new key value database.
     *
     * @return KeyValueDataBase
     */
    public KeyValueDataBase newDatabase() {
        return new RocksDatabase();
    }
}
