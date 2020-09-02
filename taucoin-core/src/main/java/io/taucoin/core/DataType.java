package io.taucoin.core;

public enum DataType {
    TIP_BLOCK, // mutable block
    TIP_TX, // mutable tx

    BLOCK, // immutable block
    TX, // immutable tx

    BLOCK_FOR_SYNC, // immutable block for sync
    TX_FOR_SYNC, // immutable tx for sync

    TIP_BLOCK_FOR_VOTING, // mutable block for voting
    BLOCK_FOR_VOTING, // immutable block for voting

    TIP_TX_FOR_POOL, // mutable tx for pool
    TX_FOR_POOL, // immutable tx for pool
}
