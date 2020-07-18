package io.taucoin.core;

import io.taucoin.db.StateDB;
import io.taucoin.types.Block;
import io.taucoin.types.MsgType;
import io.taucoin.types.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaucoinStateProcessor implements StateProcessor {

    private static final Logger logger = LoggerFactory.getLogger("TauStateProcessor");

    /**
     * process block
     *
     * @param block      to be processed
     * @param stateDB : state db
     * @return
     */
    @Override
    public boolean process(Block block, StateDB stateDB) {
        // check balance and nonce, then update state
        Transaction tx = block.getTxMsg();
        switch (tx.getTxData().getMsgType()) {
            case Wiring: {
                break;
            }
            case CommunityAnnouncement: {
                break;
            }
            case IdentityAnnouncement: {
                break;
            }
            default:{
                return false;
            }
        }

        //
        return false;
    }


    /**
     * roll back a block
     *
     * @param block
     * @param stateDB
     * @return
     */
    @Override
    public boolean rollback(Block block, StateDB stateDB) {
        return false;
    }
}
