package io.taucoin.core;

import io.taucoin.db.Repository;
import io.taucoin.util.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TxNoncer {
    private static final Logger logger = LoggerFactory.getLogger("TxNoncer");

    byte[] chainID;
    Repository repository;
    Map<ByteArrayWrapper, Long> nonces;

    private TxNoncer() {}

    public TxNoncer(byte[] chainID, Repository repository) {
        this.chainID = chainID;
        this.repository = repository;
        this.nonces = new HashMap<>();
    }

    /**
     * get nonce by pubKey, first find in cache, then in repo
     * @param pubKey
     * @return
     */
    public long getNonce(byte[] pubKey) {
        ByteArrayWrapper account = new ByteArrayWrapper(pubKey);
        Long nonce = nonces.get(account);
        if(null == nonce) {
            long nonceInDB;
            try {
                nonceInDB = this.repository.getNonce(chainID, pubKey).longValue();
            } catch (Exception e) {
                logger.info(e.getMessage(), e);
                nonceInDB = (long)0;
            }
            nonces.put(account, nonceInDB);
            nonce = nonceInDB;
        }

        return nonce;
    }

    /**
     * set nonce in cache
     * @param pubKey
     * @param nonce
     */
    public void setNonce(byte[] pubKey, long nonce) {
        nonces.put(new ByteArrayWrapper(pubKey), nonce);
    }

}

