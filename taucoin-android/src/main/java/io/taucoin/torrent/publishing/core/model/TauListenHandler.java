package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import androidx.annotation.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.genesis.GenesisItem;
import io.taucoin.param.ChainParam;
import io.taucoin.torrent.publishing.core.storage.sqlite.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.types.Block;
import io.taucoin.types.GenesisTx;
import io.taucoin.types.Transaction;
import io.taucoin.types.WiringCoinsTx;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;

/**
 * TauListener处理程序
 */
class TauListenHandler {
    private static final Logger logger = LoggerFactory.getLogger("TauListenHandler");
    private CompositeDisposable disposables = new CompositeDisposable();
    private UserRepository userRepo;
    private MemberRepository memberRepo;
    private TxRepository txRepo;
    private CommunityRepository communityRepo;
    private TauDaemon daemon;

    TauListenHandler(Context appContext, TauDaemon daemon){
        this.daemon = daemon;
        userRepo = RepositoryHelper.getUserRepository(appContext);
        memberRepo = RepositoryHelper.getMemberRepository(appContext);
        txRepo = RepositoryHelper.getTxRepository(appContext);
        communityRepo = RepositoryHelper.getCommunityRepository(appContext);
    }

    /**
     * 保存社区：查询本地是否有此社区，没有则添加到本地
     * @param block 链上区块
     */
    private void saveCommunityInfo(String chainID, Block block, boolean isSync) {
        disposables.add(communityRepo.getCommunityByChainIDSingle(chainID)
                .subscribeOn(Schedulers.io())
                .subscribe(community -> {
                    if(null == community){
                        community = new Community();
                        community.chainID = chainID;
                        community.communityName = Utils.getCommunityName(community.chainID);
                        community.totalBlocks = block.getBlockNum();
                        community.syncBlock = block.getBlockNum();
                        communityRepo.addCommunity(community);
                        logger.info("SaveCommunity to local, communityName::{}, chainID::{}, totalBlocks::{}, syncBlock::{}",
                                community.communityName, community.chainID, community.totalBlocks, community.syncBlock);
                    }else {
                        if(isSync){
                            community.syncBlock = block.getBlockNum();
                        }else{
                            community.totalBlocks = block.getBlockNum();
                        }
                        communityRepo.addCommunity(community);
                        logger.info("Update Community Info, communityName::{}, chainID::{}, totalBlocks::{}, syncBlock::{}",
                                community.communityName, community.chainID, community.totalBlocks, community.syncBlock);
                    }
                }));
    }
    /**
     * 处理Block数据：解析Block数据，处理社区、交易、用户、社区成员数据
     * 0、更新社区信息
     * 1、处理矿工用户数据
     * 2、本地不存在该交易，添加交易数据、用户数据、社区成员数据
     * 3、本地存在该交易，更新交易状态、以及成员的balance和power值
     * @param chainID 链上ID
     * @param block 链上区块
     * @param isRollback 区块回滚
     * @param isSync 区块向前同步
     */
    private void handleBlockData(String chainID, Block block, boolean isRollback, boolean isSync) {
        if(block != null){
            // 更新社区信息
            saveCommunityInfo(chainID, block, isSync);
            // TODO: 根据交易hash获取Transaction
            Transaction txMsg = null;
            // 更新矿工的信息
            saveUserInfo(block.getMinerPubkey(), block.getTimeStamp());
            if(txMsg != null){
                String txID = ByteUtil.toHexString(txMsg.getTxID());
                disposables.add(txRepo.getTxByTxIDSingle(txID)
                        .subscribeOn(Schedulers.io())
                        .subscribe(tx -> {
                            // 本地不存在此交易
                            if(null == tx){
                                handleTransactionData(txMsg, isRollback, isSync);
                            }else{
                                tx.txStatus = isRollback ? 0 : 1;
                                txRepo.updateTransaction(tx);
                                handleMemberInfo(txMsg);
                            }
                        }));
            }
        }
    }

    /**
     * 处理上报新的区块
     * @param chainID chainID
     * @param block Block
     */
    void handleNewBlock(String chainID, Block block) {
        handleBlockData(chainID, block, false, false);
    }
    /**
     * 处理上报被回滚的区块
     * @param chainID chainID
     * @param block Block
     */
    void handleRollBack(String chainID, Block block) {
        handleBlockData(chainID, block, true, false);
    }
    /**
     * 处理上报向前同步的区块
     * @param chainID chainID
     * @param block Block
     */
    void handleSyncBlock(String chainID, Block block) {
        handleBlockData(chainID, block, false, true);
    }

    /**
     * 处理Block数据：解析Block数据，更新用户、社区成员、交易数据
     * @param txMsg Transaction
     */
    private void handleTransactionData(@NonNull Transaction txMsg, boolean isRollback, boolean isSync) {
        // TODO: 根据hash获取交易Transaction
        String txID = ByteUtil.toHexString(txMsg.getTxID());
        String chainID = ByteUtil.toHexString(txMsg.getChainID());
        long fee = txMsg.getTxFee();
        final long txType = txMsg.getTxType();
        Tx tx = new Tx(txID, chainID, fee, txType);
        tx.senderPk = ByteUtil.toHexString(txMsg.getSenderPubkey());
        tx.txStatus = isRollback ? 0 : 1;
        if (txType == ChainParam.TxType.FNoteType.ordinal()){
            // 保存用户信息
            saveUserInfo(txMsg.getSenderPubkey(), txMsg.getTimeStamp());
            // 添加社区成员
            addMemberInfo(txMsg.getChainID(), txMsg.getSenderPubkey(), isSync);
            // 添加交易
            // TODO: 处理note hash
            tx.memo = "";
            txRepo.addTransaction(tx);
            logger.info("Add transaction to local, txID::{}, txType::{}", txID, tx.txType);
        } else if (txType == ChainParam.TxType.WCoinsType.ordinal()){
            // 保存用户信息
            WiringCoinsTx wiringTx = (WiringCoinsTx) txMsg;
            saveUserInfo(txMsg.getSenderPubkey(), txMsg.getTimeStamp());
            saveUserInfo(wiringTx.getReceiver(), 0);
            // 添加社区成员
            addMemberInfo(txMsg.getChainID(), txMsg.getSenderPubkey(), isSync);
            addMemberInfo(txMsg.getChainID(), wiringTx.getReceiver(), isSync);

            // 添加交易
            tx.receiverPk = ByteUtil.toHexString(wiringTx.getReceiver());
            tx.amount = wiringTx.getAmount();
            txRepo.addTransaction(tx);
            logger.info("Add transaction to local, txID::{}, txType::{}", txID, tx.txType);
        } else if (txType == ChainParam.TxType.GMsgType.ordinal()){
            GenesisTx genesisTx = (GenesisTx) txMsg;
            Map<ByteArrayWrapper, GenesisItem> genesisMsgKV = genesisTx.getGenesisAccounts();
            if(genesisMsgKV != null){
                for (ByteArrayWrapper key: genesisMsgKV.keySet()) {
                    String publicKey = ByteUtil.toHexString(key.getData());
                    saveUserInfo(publicKey, 0);
                    addMemberInfo(chainID, publicKey, isSync);
                }
            }
        }
    }

    /**
     * 处理社区成员信息
     * @param txMsg 交易
     */
    private void handleMemberInfo(@NonNull Transaction txMsg) {
        String chainID = ByteUtil.toHexString(txMsg.getChainID());
        long txType = txMsg.getTxType();
        if (txType == ChainParam.TxType.FNoteType.ordinal()){
            addMemberInfo(txMsg.getChainID(), txMsg.getSenderPubkey(), false);
        } else if (txType == ChainParam.TxType.WCoinsType.ordinal()) {
            WiringCoinsTx tx = (WiringCoinsTx) txMsg;
            addMemberInfo(txMsg.getChainID(), txMsg.getSenderPubkey(), false);
            addMemberInfo(txMsg.getChainID(), tx.getReceiver(), false);
        }else if (txType == ChainParam.TxType.GMsgType.ordinal()){
            GenesisTx genesisTx = (GenesisTx) txMsg;
            Map<ByteArrayWrapper, GenesisItem> genesisMsgKV = genesisTx.getGenesisAccounts();
            if(genesisMsgKV != null){
                for (ByteArrayWrapper key: genesisMsgKV.keySet()) {
                    String publicKey = ByteUtil.toHexString(key.getData());
                    addMemberInfo(chainID, publicKey, false);
                }
            }
        }
    }

    /**
     * 保存用户信息到本地
     * @param publicKey 公钥
     * @param timeStamp 时间戳
     */
    private void saveUserInfo(byte[] publicKey, long timeStamp) {
        saveUserInfo(ByteUtil.toHexString(publicKey), timeStamp);
    }

    /**
     * 保存用户信息到本地
     * @param publicKey 公钥
     * @param timeStamp 时间戳
     */
    private void saveUserInfo(String publicKey, long timeStamp) {
        User user = userRepo.getUserByPublicKey(publicKey);
        if(null == user){
            user = new User(publicKey);
            user.lastUpdateTime = timeStamp;
            userRepo.addUser(user);
            logger.info("SaveUserInfo to local, publicKey::{}, onlineTime::{}",
                    publicKey, timeStamp);
        }else{
            if(user.lastUpdateTime < timeStamp){
                user.lastUpdateTime = timeStamp;
                userRepo.addUser(user);
                logger.info("Update onlineTime, publicKey::{}, onlineTime::{}",
                        publicKey, timeStamp);
            }
        }
    }

    /**
     * 添加社区成员到本地
     * @param publicKey 公钥
     * @param chainID chainID
     */
    private void addMemberInfo(byte[] chainID, byte[] publicKey, boolean isSync) {
        addMemberInfo(ByteUtil.toHexString(chainID), ByteUtil.toHexString(publicKey), isSync);
    }

    /**
     * 添加社区成员到本地
     * @param publicKey 公钥
     * @param chainID chainID
     */
    private void addMemberInfo(String chainID, String publicKey, boolean isSync) {
        Member member = memberRepo.getMemberByChainIDAndPk(chainID, publicKey);
        long balance = daemon.getUserBalance(chainID, publicKey);
        long power = daemon.getUserPower(chainID, publicKey);
        if(null == member){
            member = new Member(chainID, publicKey, balance, power);
            memberRepo.addMember(member);
            logger.info("AddMemberInfo to local, chainID::{}, publicKey::{}, balance::{}, power::{}",
                    chainID, publicKey, balance, power);
        }else{
            if(!isSync){
                member.balance = balance;
                member.power = power;
                memberRepo.updateMember(member);
                logger.info("Update Member's balance and power, chainID::{}, publicKey::{}, balance::{}, power::{}",
                        chainID, publicKey, member.balance, member.power);
            }
        }
    }

    /**
     * 销毁处理程序
     */
    void destroy() {
        disposables.clear();
    }
}