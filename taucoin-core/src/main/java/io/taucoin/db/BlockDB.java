package io.taucoin.db;

import io.taucoin.datasource.KeyValueDataSource;

public class BlockDB {

    private KeyValueDataSource db;

    public BlockDB(KeyValueDataSource db) {
        this.db = db;
    }
}
