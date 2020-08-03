package io.taucoin.processor;

import io.taucoin.core.AccountState;
import io.taucoin.core.ImportResult;
import io.taucoin.db.StateDB;
import io.taucoin.types.Block;
import io.taucoin.types.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import static io.taucoin.core.ImportResult.*;

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
    public ImportResult forwardProcess(Block block, StateDB stateDB) {
        // check balance and nonce, then update state
        try {
            Transaction tx = block.getTxMsg();
            if (!tx.isTxParamValidate()) {
                logger.error("Tx validate fail!");
                return INVALID_BLOCK;
            }

            if (!tx.verifyTransactionSig()) {
                logger.error("Bad Signature.");
                return INVALID_BLOCK;
            }

            byte[] sender = tx.getSenderPubkey();

            AccountState sendState = stateDB.getAccount(chainID, sender);
            if (null == sendState) {
                logger.error("Cannot find account[{}] state.", Hex.toHexString(sender));
                return NO_ACCOUNT_INFO;
            } else {
                if (sendState.getNonce().compareTo(BigInteger.ZERO) == 0) {
                    logger.error("Cannot find account[{}] nonce info.", Hex.toHexString(sender));
                    return NO_ACCOUNT_INFO;
                }
            }

            // check nonce
            if (sendState.getNonce().longValue() + 1 != tx.getNonce()) {
                logger.error("Account:{} nonce mismatch!", Hex.toHexString(sender));
                return INVALID_BLOCK;
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
                        return INVALID_BLOCK;
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
//                case CommunityAnnouncement: {
//                    // check balance
//                    if (sendState.getBalance().longValue() < fee) {
//                        logger.error("No enough balance: require: {}, sender's balance: {}, txid: {}, sender:{}",
//                                fee, sendState.getBalance(), Hex.toHexString(sender));
//                        return INVALID_BLOCK;
//                    }
//
//                    //Execute the transaction
//                    // miner
//                    AccountState minerState = stateDB.getAccount(chainID, block.getMinerPubkey());
//                    minerState.addBalance(BigInteger.valueOf(fee));
//                    stateDB.updateAccount(chainID, block.getMinerPubkey(), minerState);
//                    // sender
//                    sendState.subBalance(BigInteger.valueOf(fee));
//                    sendState.increaseNonce();
//                    stateDB.updateAccount(chainID, sender, sendState);
//                    // TODO: announce app
//                    break;
//                }
                case ForumComment: {
                    // check balance
                    if (sendState.getBalance().longValue() < fee) {
                        logger.error("No enough balance: require: {}, sender's balance: {}, txid: {}, sender:{}",
                                fee, sendState.getBalance(), Hex.toHexString(sender));
                        return INVALID_BLOCK;
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
                        return INVALID_BLOCK;
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
//                case IdentityAnnouncement: {
//                    // check balance
//                    if (sendState.getBalance().longValue() < fee) {
//                        logger.error("No enough balance: require: {}, sender's balance: {}, txid: {}, sender:{}",
//                                fee, sendState.getBalance(), Hex.toHexString(sender));
//                        return false;
//                    }
//
//                    //Execute the transaction
//                    // miner
//                    AccountState minerState = stateDB.getAccount(this.chainID, block.getMinerPubkey());
//                    minerState.addBalance(BigInteger.valueOf(fee));
//                    stateDB.updateAccount(this.chainID, block.getMinerPubkey(), minerState);
//                    // sender
//                    sendState.subBalance(BigInteger.valueOf(fee));
//                    sendState.increaseNonce();
//                    sendState.setIdentity(tx.getTxData().getIdentityAnnouncementName());
//                    stateDB.updateAccount(this.chainID, sender, sendState);
//                    // TODO: announce app
//                    break;
//                }
                default: {
                    logger.error("Transaction type not supported");
                    return INVALID_BLOCK;
                }
            }
        } catch (Exception e) {
        logger.error(e.getMessage(), e);
        return EXCEPTION;
    }

        return IMPORTED_BEST;
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
            byte[] miner = block.getMinerPubkey();

            AccountState minerState = stateDB.getAccount(this.chainID, miner);
            AccountState senderState = stateDB.getAccount(this.chainID, sender);

            long minerBalance = block.getMinerBalance();
            long senderBalance = block.getSenderBalance();
            long senderNonce = block.getSenderNonce();
            long receiverBalance = block.getReceiverBalance();

            // if not existed , update it
            if (null == senderState) {
                senderState = new AccountState(BigInteger.valueOf(senderBalance), BigInteger.valueOf(senderNonce));
//                if (tx.getTxData().getMsgType() == MsgType.IdentityAnnouncement) {
//                    senderState.setIdentity(tx.getTxData().getIdentityAnnouncementName());
//                }
            } else {
                // sender账户存在，则不用管miner和receiver与sender是同一账户的情况
                // if account is existed, but nonce is null, update nonce
                if (0 == senderState.getNonce().longValue()) {
                    senderState.setNonce(BigInteger.valueOf(senderNonce));
                }

                switch (tx.getTxData().getMsgType()) {
                    case Wiring: {
                        AccountState receiverState = stateDB.getAccount(this.chainID, tx.getTxData().getReceiver());
                        // receiver账户状态不存在与receiver == sender，两种情况不会同时发生
                        if (null == receiverState) {
                            // 执行到这里，说明receiver != sender
                            receiverState = new AccountState(BigInteger.valueOf(receiverBalance), BigInteger.ZERO);
                            stateDB.updateAccount(this.chainID, tx.getTxData().getReceiver(), receiverState);
                        }
                        break;
                    }
//                    case IdentityAnnouncement: {
//                        if (null == senderState.getIdentity()) {
//                            senderState.setIdentity(tx.getTxData().getIdentityAnnouncementName());
//                        }
//                        break;
//                    }
                    default: {

                    }
                }
            }

            // update sender state
            stateDB.updateAccount(this.chainID, sender, senderState);

            // if miner != sender && null, update miner
            if (!Arrays.areEqual(sender, block.getMinerPubkey()) && null == minerState) {
                minerState = new AccountState(BigInteger.valueOf(minerBalance), BigInteger.ZERO);
                stateDB.updateAccount(this.chainID, miner, minerState);
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

                    // roll back the transaction
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
//                case CommunityAnnouncement: {
//                    // roll back the transaction
//                    // miner
//                    AccountState minerState = stateDB.getAccount(chainID, block.getMinerPubkey());
//                    minerState.subBalance(BigInteger.valueOf(fee));
//                    stateDB.updateAccount(chainID, block.getMinerPubkey(), minerState);
//                    // sender
//                    sendState.addBalance(BigInteger.valueOf(fee));
//                    sendState.reduceNonce();
//                    stateDB.updateAccount(chainID, sender, sendState);
//                    // TODO:: announcement app
//                    break;
//                }
                case ForumComment: {
                    // roll back the transaction
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
                    // roll back the transaction
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
//                case IdentityAnnouncement: {
//                    // roll back the transaction
//                    // miner
//                    AccountState minerState = stateDB.getAccount(chainID, block.getMinerPubkey());
//                    minerState.subBalance(BigInteger.valueOf(fee));
//                    stateDB.updateAccount(chainID, block.getMinerPubkey(), minerState);
//                    // sender
//                    // note: don't need to roll back identity
//                    sendState.addBalance(BigInteger.valueOf(fee));
//                    sendState.reduceNonce();
//                    stateDB.updateAccount(chainID, sender, sendState);
//                    // TODO: announce app
//                    break;
//                }
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
