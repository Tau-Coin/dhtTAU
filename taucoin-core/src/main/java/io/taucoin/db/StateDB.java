package io.taucoin.db;

import io.taucoin.datasource.KeyValueDataSource;

public class StateDB {

    private KeyValueDataSource db;

    public StateDB(KeyValueDataSource db) {
        this.db = db;
    }
}
