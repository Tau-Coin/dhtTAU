package io.taucoin.types;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class IndexMutableData {
    private BigInteger timestamp;
    private List<byte[]> hashList = new CopyOnWriteArrayList<>();
}
