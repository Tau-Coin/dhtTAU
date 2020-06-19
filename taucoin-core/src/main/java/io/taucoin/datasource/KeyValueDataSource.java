package io.taucoin.datasource;

import java.util.Map;

public interface KeyValueDataSource extends DataSource {

    byte[] get(byte[] key);

    byte[] put(byte[] key, byte[] value);

    void delete(byte[] key);

    void updateBatch(Map<byte[], byte[]> rows);

    void removeWithPrefix(byte[] prefix);
}
