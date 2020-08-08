package io.taucoin.torrent.publishing.core.model;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import androidx.annotation.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.genesis.GenesisItem;
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
import io.taucoin.types.MsgType;
import io.taucoin.types.Transaction;
import io.taucoin.types.TxData;
import io.taucoin.types.WireTransaction;
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
    private void saveCommunityInfo(Block block, boolean isSync) {
        String chainID = ByteUtil.toHexString(block.getChainID());
        disposables.add(communityRepo.getCommunityByChainIDSingle(chainID)
                .subscribeOn(Schedulers.io())
                .subscribe(community -> {
                    if(null == community){
                        community = new Community();
                        community.chainID = ByteUtil.toHexString(block.getChainID());
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
     * @param block 链上区块
     */
    void handleBlockData(Block block, boolean isRollback, boolean isSync) {
        if(block != null){
            // 更新社区信息
            saveCommunityInfo(block, isSync);
            Transaction txMsg = block.getTxMsg();
            // 更新矿工的信息
            saveUserInfo(block.getMinerPubkey(), block.getTimeStamp());
            if(txMsg != null && txMsg.getTxData() != null){
                String txID = ByteUtil.toHexString(txMsg.getTxID());
                disposables.add(txRepo.getTxByTxIDSingle(txID)
                        .subscribeOn(Schedulers.io())
                        .subscribe(tx -> {
                            // 本地不存在此交易
                            if(null == tx){
                                handleTransactionData(block, isRollback, isSync);
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
     * 处理Block数据：解析Block数据，更新用户、社区成员、交易数据
     * @param block Block
     */
    private void handleTransactionData(@NonNull Block block, boolean isRollback, boolean isSync) {
        Transaction txMsg = block.getTxMsg();
        String txID = ByteUtil.toHexString(txMsg.getTxID());
        String chainID = ByteUtil.toHexString(txMsg.getChainID());
        long fee = txMsg.getTxFee();
        TxData txData = txMsg.getTxData();
        MsgType msgType = txData.getMsgType();
        Tx tx = new Tx(txID, chainID, fee, msgType.getVaLue());
        tx.senderPk = ByteUtil.toHexString(txMsg.getSenderPubkey());
        tx.txStatus = isRollback ? 0 : 1;
        switch (msgType){
            case RegularForum:
                // 保存用户信息
                saveUserInfo(txMsg.getSenderPubkey(), txMsg.getTimeStamp());
                // 添加社区成员
                addMemberInfo(txMsg.getChainID(), txMsg.getSenderPubkey(), isSync);
                // 添加交易
                tx.memo = txData.getNoteMsg();
                txRepo.addTransaction(tx);
                logger.info("Add transaction to local, txID::{}, txType::{}", txID, tx.txType);
                break;
            case Wiring:
                // 保存用户信息
                saveUserInfo(txMsg.getSenderPubkey(), txMsg.getTimeStamp());
                saveUserInfo(txData.getReceiver(), 0);
                // 添加社区成员
                addMemberInfo(txMsg.getChainID(), txMsg.getSenderPubkey(), isSync);
                addMemberInfo(txMsg.getChainID(), txData.getReceiver(), isSync);

                // 添加交易
                WireTransaction wtx = new WireTransaction(txData.getTxCode());
                tx.receiverPk = ByteUtil.toHexString(txData.getReceiver());
                tx.amount = txData.getAmount();
                tx.memo = wtx.getNotes();
                txRepo.addTransaction(tx);
                logger.info("Add transaction to local, txID::{}, txType::{}", txID, tx.txType);
                break;
            case GenesisMsg:
                Map<String, GenesisItem> genesisMsgKV = txData.getGenesisMsgKV();
                if(genesisMsgKV != null){
                    for (String key: genesisMsgKV.keySet()) {
                        saveUserInfo(key, 0);
                        addMemberInfo(chainID, key, isSync);
                    }
                }
                break;
        }
    }

    /**
     * 处理社区成员信息
     * @param txMsg 交易
     */
    private void handleMemberInfo(@NonNull Transaction txMsg) {
        String chainID = ByteUtil.toHexString(txMsg.getChainID());
        TxData txData = txMsg.getTxData();
        MsgType msgType = txData.getMsgType();
        switch (msgType){
            case RegularForum:
                addMemberInfo(txMsg.getChainID(), txMsg.getSenderPubkey(), false);
                break;
            case Wiring:
                addMemberInfo(txMsg.getChainID(), txMsg.getSenderPubkey(), false);
                addMemberInfo(txMsg.getChainID(), txData.getReceiver(), false);
                break;
            case GenesisMsg:
                Map<String, GenesisItem> genesisMsgKV = txData.getGenesisMsgKV();
                if(genesisMsgKV != null){
                    for (String key: genesisMsgKV.keySet()) {
                        addMemberInfo(chainID, key, false);
                    }
                }
                break;
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