package io.taucoin.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MessageDBImpl implements MessageDB {
    private static final Logger logger = LoggerFactory.getLogger("MsgDBImpl");

    private final KeyValueDataBase db;

    public MessageDBImpl(KeyValueDataBase db) {
        this.db = db;
    }

    /**
     * open db
     * @param path database path which can be accessed
     * @throws DBException database exception
     */
    public void open(String path) throws DBException {
        try {
            db.open(path);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * close db
     */
    public void close() {
        db.close();
    }

    /**
     * add a new friend
     *
     * @param pubkey public key
     * @throws DBException database exception
     */
    @Override
    public void addFriend(byte[] pubkey) throws DBException {
        try {
            db.put(PrefixKey.friendKey(pubkey), new byte[1]);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get all friends
     *
     * @return friend set
     * @throws DBException database exception
     */
    @Override
    public Set<byte[]> getFriends() throws DBException {
        Set<byte[]> ret = new HashSet<>();
        byte[] prefix = PrefixKey.friendPrefix();

        Set<byte[]> set;
        try {
            set = db.retrieveKeysWithPrefix(prefix);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }

        if (null != set) {
            for(byte[] friend: set) {
                ret.add(Arrays.copyOfRange(friend, prefix.length, friend.length));
            }
        }

        return ret;
    }

    /**
     * delete a friend
     *
     * @param pubKey public key
     * @throws DBException database exception
     */
    @Override
    public void delFriend(byte[] pubKey) throws DBException {
        try {
            db.delete(PrefixKey.friendKey(pubKey));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * set friend message root hash
     *
     * @param pubKey public key
     * @param hash message root hash
     * @throws DBException database exception database exception
     */
    @Override
    public void setFriendMessageRoot(byte[] pubKey, byte[] hash) throws DBException {
        try {
            db.put(PrefixKey.friendMessageRootKey(pubKey), hash);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get friend message root hash
     *
     * @param pubKey public key
     * @return friend message root hash, null otherwise
     * @throws DBException database exception database exception
     */
    @Override
    public byte[] getFriendMessageRoot(byte[] pubKey) throws DBException {
        try {
            return db.get(PrefixKey.friendMessageRootKey(pubKey));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * put a message data into db
     *
     * @param hash message hash
     * @param data message data to put
     * @throws DBException database exception database exception
     */
    @Override
    public void putMessage(byte[] hash, byte[] data) throws DBException {
        try {
            db.put(PrefixKey.messageKey(hash), data);
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }

    /**
     * get message by hash
     *
     * @param hash message hash
     * @return message data, null otherwise
     * @throws DBException database exception database exception
     */
    @Override
    public byte[] getMessageByHash(byte[] hash) throws DBException {
        try {
            return db.get(PrefixKey.messageKey(hash));
        } catch (Exception e) {
            throw new DBException(e.getMessage());
        }
    }
}
