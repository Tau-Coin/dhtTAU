package io.taucoin.core;

public enum ImportResult {

    IMPORTED_BEST,
    IMPORTED_NOT_BEST,
    EXIST,
    NO_PARENT,
    NO_ACCOUNT_INFO,
    INVALID_BLOCK,
    INVALID_TX,
    CONSENSUS_BREAK,
    EXCEPTION;

    public boolean isSuccessful() {
        return equals(IMPORTED_BEST) || equals(IMPORTED_NOT_BEST);
    }
}
