package io.taucoin.db;

import java.util.Set;

public interface MessageDB {
    /**
     * Open database.
     *
     * @param path database path which can be accessed
     * @throws DBException database exception
     */
    void open(String path) throws DBException;

    /**
     * Close database.
     */
    void close();

    /**
     * add a new friend
     * @param pubkey public key
     * @throws DBException database exception
     */
    void addFriend(byte[] pubkey) throws DBException;

    /**
     * get all friends
     * @return friend set
     * @throws DBException database exception
     */
    Set<byte[]> getFriends() throws DBException;

    /**
     * delete a friend
     * @param pubKey public key
     * @throws DBException database exception
     */
    void delFriend(byte[] pubKey) throws DBException;

    /**
     * set friend message root hash
     * @param pubKey public key
     * @param hash message root hash
     * @throws DBException database exception database exception
     */
    void setFriendMessageRoot(byte[] pubKey, byte[] hash) throws DBException;

    /**
     * get friend message root hash
     * @param pubKey public key
     * @return friend message root hash, null otherwise
     * @throws DBException database exception database exception
     */
    byte[] getFriendMessageRoot(byte[] pubKey) throws DBException;

    /**
     * set message to friend root hash
     * @param pubKey public key
     * @param hash message root hash
     * @throws DBException database exception database exception
     */
    void setMessageToFriendRoot(byte[] pubKey, byte[] hash) throws DBException;

    /**
     * get message to friend root hash
     * @param pubKey public key
     * @return friend message root hash, null otherwise
     * @throws DBException database exception database exception
     */
    byte[] getMessageToFriendRoot(byte[] pubKey) throws DBException;

    /**
     * put a message data into db
     * @param hash message hash
     * @param data message data to put
     * @throws DBException database exception database exception
     */
    void putMessage(byte[] hash, byte[] data) throws DBException;

    /**
     * get message by hash
     * @param hash message hash
     * @return message data, null otherwise
     * @throws DBException database exception database exception
     */
    byte[] getMessageByHash(byte[] hash) throws DBException;
}
