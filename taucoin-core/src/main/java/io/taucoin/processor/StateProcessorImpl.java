package io.taucoin.processor;

import io.taucoin.core.AccountState;
import io.taucoin.types.BlockContainer;
import io.taucoin.core.ImportResult;
import io.taucoin.db.StateDB;
import io.taucoin.genesis.GenesisItem;
import io.taucoin.types.TypesConfig;
import io.taucoin.types.Block;
import io.taucoin.types.GenesisTx;
import io.taucoin.types.Transaction;
import io.taucoin.types.WiringCoinsTx;
import io.taucoin.util.ByteArrayWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.HashMap;

import static io.taucoin.core.ImportResult.*;

public class StateProcessorImpl implements StateProcessor {

    private static final Logger logger = LoggerFactory.getLogger("StateProcessor");

    private final byte[] chainID;

    public StateProcessorImpl(byte[] chainID) {
        this.chainID = chainID;
    }

    /**
     * forward process block, when sync new block
     *
     * @param blockContainer      to be processed
     * @param stateDB : state db
     * @return import result
     */
    @Override
    public ImportResult forwardProcess(BlockContainer blockContainer, StateDB stateDB) {
        // check balance and nonce, then update state
        try {
            Block block = blockContainer.getBlock();

            Transaction tx = blockContainer.getTx();
            if (null != tx) {
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

                if (TypesConfig.TxType.WCoinsType.ordinal() == tx.getTxType()) {
                    // check balance
                    long amount = ((WiringCoinsTx)tx).getAmount();
                    long cost = amount + fee;
                    if (sendState.getBalance().longValue() < cost) {
                        logger.error("No enough balance: require: {}, sender's balance: {}, txid: {}, sender:{}",
                                cost, sendState.getBalance(), Hex.toHexString(tx.getTxID()), Hex.toHexString(sender));
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
                    byte[] receiver = ((WiringCoinsTx)tx).getReceiver();
                    AccountState receiverState = stateDB.getAccount(chainID, receiver);
                    receiverState.addBalance(BigInteger.valueOf(amount));
                    stateDB.updateAccount(chainID, receiver, receiverState);
                } else if (TypesConfig.TxType.FNoteType.ordinal() == tx.getTxType()) {
                    // check balance
                    if (sendState.getBalance().longValue() < fee) {
                        logger.error("No enough balance: require: {}, sender's balance: {}, txid: {}, sender:{}",
                                fee, sendState.getBalance(), Hex.toHexString(tx.getTxID()), Hex.toHexString(sender));
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
                } else {
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
     * sync genesis block
     *
     * @param blockContainer genesis block container
     * @param stateDB state db
     * @return import result
     */
    @Override
    public ImportResult backwardProcessGenesisBlock(BlockContainer blockContainer, StateDB stateDB) {
        try {
            Transaction tx = blockContainer.getTx();
            if (null == tx) {
                logger.error("Tx is null!");
                return INVALID_BLOCK;
            }

            if (!tx.isTxParamValidate()) {
                logger.error("Tx validate fail!");
                return INVALID_BLOCK;
            }
            // TODO:: verify
            /*
            if (!tx.verifyTransactionSig()) {
                logger.error("Bad Signature.");
                return false;
            }

            // if genesis block -> msg type == error
            if (block.getBlockNum() != 0 || MsgType.GenesisMsg == tx.getTxData().getMsgType()) {
                logger.error("Genesis type error!");
                return false;
            }
            */


            HashMap<ByteArrayWrapper, GenesisItem> map = ((GenesisTx)tx).getGenesisAccounts();
            if (null != map) {
                for (HashMap.Entry<ByteArrayWrapper, GenesisItem> entry : map.entrySet()) {
                    byte[] pubKey = entry.getKey().getData();
                    AccountState accountState = stateDB.getAccount(this.chainID, pubKey);
                    if (null == accountState) {
                        accountState = new AccountState(entry.getValue().getBalance(),
                                entry.getValue().getPower());
                        stateDB.updateAccount(this.chainID, pubKey, accountState);
                    } else {
                        if (0 == accountState.getNonce().longValue()) {
                            accountState.setNonce(entry.getValue().getPower());
                            stateDB.updateAccount(this.chainID, pubKey, accountState);
                        }
                    }
                }
            } else {
                logger.error("Cannot found genesis account.");
                return INVALID_BLOCK;
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
     * @param blockContainer   block to be processed
     * @param stateDB state db
     * @return import result
     */
    @Override
    public ImportResult backwardProcess(BlockContainer blockContainer, StateDB stateDB) {

        Block block = blockContainer.getBlock();
        // if genesis block
        if (block.getBlockNum() == 0) {
            return backwardProcessGenesisBlock(blockContainer, stateDB);
        }

        // check balance and nonce, then update state
        try {

            Transaction tx = blockContainer.getTx();

            if (null != tx) {
                // TODO:: type match?
                if (!tx.isTxParamValidate()) {
                    logger.error("Tx validate fail!");
                    return INVALID_BLOCK;
                }

                if (!tx.verifyTransactionSig()) {
                    logger.error("Bad Signature.");
                    return INVALID_BLOCK;
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

                    if (TypesConfig.TxType.WCoinsType.ordinal() == tx.getTxType()) {
                        AccountState receiverState = stateDB.getAccount(this.chainID,
                                ((WiringCoinsTx)tx).getReceiver());

                        // receiver账户状态不存在与receiver == sender，两种情况不会同时发生
                        if (null == receiverState) {
                            // 执行到这里，说明receiver != sender
                            receiverState = new AccountState(BigInteger.valueOf(receiverBalance), BigInteger.ZERO);
                            stateDB.updateAccount(this.chainID, ((WiringCoinsTx)tx).getReceiver(), receiverState);
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
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return EXCEPTION;
        }

        return IMPORTED_BEST;
    }

    /**
     * roll back a block
     *
     * @param blockContainer block container
     * @param stateDB state db
     * @return true if succeed, false otherwise
     */
    @Override
    public boolean rollback(BlockContainer blockContainer, StateDB stateDB) {
        // check balance and nonce, then update state
        try {
            Transaction tx = blockContainer.getTx();

            if (null != tx) {

                byte[] sender = tx.getSenderPubkey();

                AccountState sendState = stateDB.getAccount(this.chainID, sender);
                if (null == sendState) {
                    logger.error("Cannot find account[{}] state.", Hex.toHexString(sender));
                    return false;
                }

                Block block = blockContainer.getBlock();
                long fee = tx.getTxFee();

                if (TypesConfig.TxType.WCoinsType.ordinal() == tx.getTxType()) {
                    long amount = ((WiringCoinsTx)tx).getAmount();
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
                    byte[] receiver = ((WiringCoinsTx)tx).getReceiver();
                    AccountState receiverState = stateDB.getAccount(this.chainID, receiver);
                    receiverState.subBalance(BigInteger.valueOf(amount));
                    stateDB.updateAccount(this.chainID, receiver, receiverState);
                } else {
                    // roll back the transaction
                    // miner
                    AccountState minerState = stateDB.getAccount(chainID, block.getMinerPubkey());
                    minerState.subBalance(BigInteger.valueOf(fee));
                    stateDB.updateAccount(chainID, block.getMinerPubkey(), minerState);
                    // sender
                    sendState.addBalance(BigInteger.valueOf(fee));
                    sendState.reduceNonce();
                    stateDB.updateAccount(chainID, sender, sendState);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }
}
