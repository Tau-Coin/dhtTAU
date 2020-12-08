package io.taucoin.core;

import java.util.Arrays;

public class FriendPair {
    public final byte[] sender;
    public final byte[] receiver;

    public static FriendPair create(byte[] sender, byte[] receiver) {
        return new FriendPair(sender, receiver);
    }

    /**
     * Constructor for a FriendPair.
     *
     * @param sender the first object in the Pair
     * @param receiver the second object in the pair
     */
    public FriendPair(byte[] sender, byte[] receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    public byte[] getSender() {
        return sender;
    }

    public byte[] getReceiver() {
        return receiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FriendPair friendPair = (FriendPair) o;
        return Arrays.equals(sender, friendPair.sender) &&
                Arrays.equals(receiver, friendPair.receiver);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(sender);
        result = 31 * result + Arrays.hashCode(receiver);
        return result;
    }

}
