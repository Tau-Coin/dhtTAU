package io.taucoin.chain;

public enum ForkPoint {
    WITH_MUTABLE_RANGE, // 在mutable range之内
    WITH_WARNING_RANGE, // 在1-3倍的mutable range之内
    BEYOND_WARNING_RANGE, // 在3倍的mutable range之外
}
