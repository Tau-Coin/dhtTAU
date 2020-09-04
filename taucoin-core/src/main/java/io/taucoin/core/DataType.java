package io.taucoin.core;

public enum DataType {
    TIP_BLOCK_REQUEST_FROM_PEER, // mutable block

    HISTORY_BLOCK_REQUEST, // immutable block
    HISTORY_TX_REQUEST, // immutable tx

    BLOCK_REQUEST_FROM_PEER, // mutable item: request block hash
    TX_REQUEST_FROM_PEER, // mutable item: request tx hash

    BLOCK_FOR_SYNC, // immutable block for sync
    TX_FOR_SYNC, // immutable tx for sync

    TIP_BLOCK_FOR_VOTING, // mutable block for voting
    BLOCK_FOR_VOTING, // immutable block for voting

    TIP_TX_FOR_MINING, // mutable tx for pool
    TX_FOR_MINING, // immutable tx for pool
}
