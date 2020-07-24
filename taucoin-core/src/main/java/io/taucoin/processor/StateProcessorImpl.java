package io.taucoin.processor;

import io.taucoin.core.AccountState;
import io.taucoin.db.StateDB;
import io.taucoin.types.Block;
import io.taucoin.types.MsgType;
import io.taucoin.types.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

public class StateProcessorImpl implements StateProcessor {

    private static final Logger logger = LoggerFactory.getLogger("StateProcessor");

    private byte[] chainID;

    public StateProcessorImpl(byte[] chainID) {
        this.chainID = chainID;
    }

    /**
     * forward process block, when sync new block
     *
     * @param block      to be processed
     * @param stateDB : state db
     * @return
     */
    @Override
    public boolean forwardProcess(Block block, StateDB stateDB) {
        // check balance and nonce, then update state
        try {
            Transaction tx = block.getTxMsg();
            if (!tx.isTxParamValidate()) {
                logger.error("Tx validate fail!");
                return false;
            }

            if (!tx.verifyTransactionSig()) {
                logger.error("Bad Signature.");
                return false;
            }

            byte[] sender = tx.getSenderPubkey();

            AccountState sendState = stateDB.getAccount(chainID, sender);
            if (null == sendState) {
                logger.error("Cannot find account[{}] state.", Hex.toHexString(sender));
                return false;
            }

            // check nonce
            if (sendState.getNonce().longValue() + 1 != tx.getNonce()) {
                logger.error("Account:{} nonce mismatch!", Hex.toHexString(sender));
                return false;
            }

            long fee = tx.getTxFee();

            switch (tx.getTxData().getMsgType()) {
                case Wiring: {
                    // check balance
                    long amount = tx.getTxData().getAmount();
                    long cost = amount + fee;
                    if (sendState.getBalance().longValue() < cost) {
                        logger.error("No enough balance: require: {}, sender's balance: {}, txid: {}, sender:{}",
                                cost, sendState.getBalance(), Hex.toHexString(sender));
                        return false;
                    }

                    //Execute the transaction
                    // miner
                    AccountState minerState = stateDB.getAccount(chainID, block.getMinerPubkey());
                    minerState.addBalance(BigInteger.valueOf(fee));
                    stateDB.updateAccount(chainID, block.getMinerPubkey(), minerState);
                    // sender
                    sendState.subBalance(BigInteger.valueOf(cost));
                    sendState.increaseNonce();
                    stateDB.updateAccount(chainID, sender, sendState);
                    // receiver
                    byte[] receiver = tx.getTxData().getReceiver();
                    AccountState receiverState = stateDB.getAccount(chainID, receiver);
                    receiverState.addBalance(BigInteger.valueOf(amount));
                    stateDB.updateAccount(chainID, receiver, receiverState);
                    break;
                }
                case CommunityAnnouncement: {
                    // check balance
                    if (sendState.getBalance().longValue() < fee) {
                        logger.error("No enough balance: require: {}, sender's balance: {}, txid: {}, sender:{}",
                                fee, sendState.getBalance(), Hex.toHexString(sender));
                        return false;
                    }

                    //Execute the transaction
                    // miner
                    AccountState minerState = stateDB.getAccount(chainID, block.getMinerPubkey());
                    minerState.addBalance(BigInteger.valueOf(fee));
                    stateDB.updateAccount(chainID, block.getMinerPubkey(), minerState);
                    // sender
                    sendState.subBalance(BigInteger.valueOf(fee));
                    sendState.increaseNonce();
                    stateDB.updateAccount(chainID, sender, sendState);
                    // TODO: announce app
                    break;
                }
                case ForumComment: {
                    // check balance
                    if (sendState.getBalance().longValue() < fee) {
                        logger.error("No enough balance: require: {}, sender's balance: {}, txid: {}, sender:{}",
                                fee, sendState.getBalance(), Hex.toHexString(sender));
                        return false;
                    }

                    //Execute the transaction
                    // miner
                    AccountState minerState = stateDB.getAccount(chainID, block.getMinerPubkey());
                    minerState.addBalance(BigInteger.valueOf(fee));
                    stateDB.updateAccount(chainID, block.getMinerPubkey(), minerState);
                    // sender
                    sendState.subBalance(BigInteger.valueOf(fee));
                    sendState.increaseNonce();
                    stateDB.updateAccount(chainID, sender, sendState);
                    // TODO: announce app
                    break;
                }
                case RegularForum: {
                    // check balance
                    if (sendState.getBalance().longValue() < fee) {
                        logger.error("No enough balance: require: {}, sender's balance: {}, txid: {}, sender:{}",
                                fee, sendState.getBalance(), Hex.toHexString(sender));
                        return false;
                    }

                    //Execute the transaction
                    // miner
                    AccountState minerState = stateDB.getAccount(this.chainID, block.getMinerPubkey());
                    minerState.addBalance(BigInteger.valueOf(fee));
                    stateDB.updateAccount(this.chainID, block.getMinerPubkey(), minerState);
                    // sender
                    sendState.subBalance(BigInteger.valueOf(fee));
                    sendState.increaseNonce();
                    stateDB.updateAccount(this.chainID, sender, sendState);
                    // TODO: announce app
                    break;
                }
                case IdentityAnnouncement: {
                    // check balance
                    if (sendState.getBalance().longValue() < fee) {
                        logger.error("No enough balance: require: {}, sender's balance: {}, txid: {}, sender:{}",
                                fee, sendState.getBalance(), Hex.toHexString(sender));
                        return false;
                    }

                    //Execute the transaction
                    // miner
                    AccountState minerState = stateDB.getAccount(this.chainID, block.getMinerPubkey());
                    minerState.addBalance(BigInteger.valueOf(fee));
                    stateDB.updateAccount(this.chainID, block.getMinerPubkey(), minerState);
                    // sender
                    sendState.subBalance(BigInteger.valueOf(fee));
                    sendState.increaseNonce();
                    sendState.setIdentity(tx.getTxData().getIdentityAnnouncementName());
                    stateDB.updateAccount(this.chainID, sender, sendState);
                    // TODO: announce app
                    break;
                }
                default: {
                    logger.error("Transaction type not supported");
                    return false;
                }
            }
        } catch (Exception e) {
        logger.error(e.getMessage(), e);
        return false;
    }

        return true;
    }

    /**
     * backward process block, when sync old block
     *
     * @param block   block to be processed
     * @param stateDB state db
     * @return
     */
    @Override
    public boolean backwardProcess(Block block, StateDB stateDB) {
        // check balance and nonce, then update state
        try {
            Transaction tx = block.getTxMsg();
            if (!tx.isTxParamValidate()) {
                logger.error("Tx validate fail!");
                return false;
            }

            if (!tx.verifyTransactionSig()) {
                logger.error("Bad Signature.");
                return false;
            }

            byte[] sender = tx.getSenderPubkey();

            AccountState sendState = stateDB.getAccount(this.chainID, sender);

            // if not existed , update it
            if (null == sendState) {
                long senderBalance = block.getSenderBalance();
                long senderNonce = block.getSenderNonce();
                sendState = new AccountState(BigInteger.valueOf(senderBalance), BigInteger.valueOf(senderNonce));
                if (tx.getTxData().getMsgType() == MsgType.IdentityAnnouncement) {
                    sendState.setIdentity(tx.getTxData().getIdentityAnnouncementName());
                }
            } else {
                if (tx.getTxData().getMsgType() == MsgType.IdentityAnnouncement
                        && null == sendState.getIdentity()) {
                    sendState.setIdentity(tx.getTxData().getIdentityAnnouncementName());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
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
        // check balance and nonce, then update state
        try {
            Transaction tx = block.getTxMsg();

            byte[] sender = tx.getSenderPubkey();

            AccountState sendState = stateDB.getAccount(this.chainID, sender);
            if (null == sendState) {
                logger.error("Cannot find account[{}] state.", Hex.toHexString(sender));
                return false;
            }

            long fee = tx.getTxFee();

            switch (tx.getTxData().getMsgType()) {
                case Wiring: {
                    long amount = tx.getTxData().getAmount();
                    long cost = amount + fee;

                    //roll back the transaction
                    // miner
                    AccountState minerState = stateDB.getAccount(this.chainID, block.getMinerPubkey());
                    minerState.subBalance(BigInteger.valueOf(fee));
                    stateDB.updateAccount(this.chainID, block.getMinerPubkey(), minerState);
                    // sender
                    sendState.addBalance(BigInteger.valueOf(cost));
                    sendState.reduceNonce();
                    stateDB.updateAccount(this.chainID, sender, sendState);
                    // receiver
                    byte[] receiver = tx.getTxData().getReceiver();
                    AccountState receiverState = stateDB.getAccount(this.chainID, receiver);
                    receiverState.subBalance(BigInteger.valueOf(amount));
                    stateDB.updateAccount(this.chainID, receiver, receiverState);
                    break;
                }
                case CommunityAnnouncement: {
                    //roll back the transaction
                    // miner
                    AccountState minerState = stateDB.getAccount(chainID, block.getMinerPubkey());
                    minerState.subBalance(BigInteger.valueOf(fee));
                    stateDB.updateAccount(chainID, block.getMinerPubkey(), minerState);
                    // sender
                    sendState.addBalance(BigInteger.valueOf(fee));
                    sendState.reduceNonce();
                    stateDB.updateAccount(chainID, sender, sendState);
                    // TODO:: announcement app
                    break;
                }
                case ForumComment: {
                    //roll back the transaction
                    // miner
                    AccountState minerState = stateDB.getAccount(chainID, block.getMinerPubkey());
                    minerState.subBalance(BigInteger.valueOf(fee));
                    stateDB.updateAccount(chainID, block.getMinerPubkey(), minerState);
                    // sender
                    sendState.addBalance(BigInteger.valueOf(fee));
                    sendState.reduceNonce();
                    stateDB.updateAccount(chainID, sender, sendState);
                    // TODO: announce app
                    break;
                }
                case RegularForum: {
                    //roll back the transaction
                    // miner
                    AccountState minerState = stateDB.getAccount(chainID, block.getMinerPubkey());
                    minerState.subBalance(BigInteger.valueOf(fee));
                    stateDB.updateAccount(chainID, block.getMinerPubkey(), minerState);
                    // sender
                    sendState.addBalance(BigInteger.valueOf(fee));
                    sendState.reduceNonce();
                    stateDB.updateAccount(chainID, sender, sendState);
                    // TODO: announce app
                    break;
                }
                case IdentityAnnouncement: {
                    //roll back the transaction
                    // miner
                    AccountState minerState = stateDB.getAccount(chainID, block.getMinerPubkey());
                    minerState.subBalance(BigInteger.valueOf(fee));
                    stateDB.updateAccount(chainID, block.getMinerPubkey(), minerState);
                    // sender
                    // note: don't need to roll back identity
                    sendState.addBalance(BigInteger.valueOf(fee));
                    sendState.reduceNonce();
                    stateDB.updateAccount(chainID, sender, sendState);
                    // TODO: announce app
                    break;
                }
                default: {
                    logger.error("Transaction type not supported");
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }
}
