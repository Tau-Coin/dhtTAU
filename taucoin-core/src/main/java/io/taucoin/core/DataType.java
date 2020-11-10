package io.taucoin.core;

public enum DataType {

    TIP_ITEM_FROM_PEER_FOR_MINING, // mutable item: tip item

    HISTORY_BLOCK_REQUEST_FOR_MINING, // immutable block
    HISTORY_HORIZONTAL_ITEM_REQUEST_FOR_MINING, // immutable horizontal item
    HISTORY_VERTICAL_ITEM_REQUEST_FOR_MINING, // immutable vertical item
    HISTORY_TX_REQUEST_FOR_MINING, // immutable tx

    DEMAND_FROM_PEER, // mutable item: request demand hash

    HISTORY_BLOCK_DEMAND, // immutable item: get block

    HISTORY_BLOCK_REQUEST_FOR_SYNC, // immutable block for sync
    HISTORY_HORIZONTAL_ITEM_REQUEST_FOR_SYNC, // immutable horizontal item for sync
    HISTORY_VERTICAL_ITEM_REQUEST_FOR_SYNC, // immutable vertical item for sync
    HISTORY_TX_REQUEST_FOR_SYNC, // immutable tx for sync

    TIP_BLOCK_FROM_PEER_FOR_VOTING, // mutable block for voting
    HISTORY_BLOCK_REQUEST_FOR_VOTING, // immutable block for voting

    TX_REQUEST_FOR_MINING, // immutable tx for pool

    PERSONAL_INFO_HASH, // personal information hash
    PERSONAL_INFO, // personal information
}
