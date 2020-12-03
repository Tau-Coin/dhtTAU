package io.taucoin.types;

public enum GossipType {
    MSG,
    DEMAND, // 仅仅针对immutable item,目前不涉及mutable demand
    UNKNOWN,
}
