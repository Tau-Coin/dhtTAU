package io.taucoin.datasource;

import java.util.Map;
import java.util.Set;

public interface KeyValueDataSource extends DataSource {

    byte[] get(byte[] key);

    byte[] put(byte[] key, byte[] value);

    void delete(byte[] key);

    void updateBatch(Map<byte[], byte[]> rows);

    Set<byte[]> seekKeysWithPrefix(byte[] prefix);

    void removeWithPrefix(byte[] prefix);
}
