package io.taucoin.chain;

import com.frostwire.jlibtorrent.Pair;
import io.taucoin.account.AccountManager;
import io.taucoin.core.AccountState;
import io.taucoin.types.BlockContainer;
import io.taucoin.types.MutableItemValue;
import io.taucoin.db.BlockDB;
import io.taucoin.db.KeyValueDataBaseFactory;
import io.taucoin.db.StateDB;
import io.taucoin.db.StateDBImpl;
import io.taucoin.genesis.GenesisConfig;
import io.taucoin.genesis.GenesisItem;
import io.taucoin.genesis.TauGenesisConfig;
import io.taucoin.genesis.TauGenesisTransaction;
import io.taucoin.listener.TauListener;
import io.taucoin.param.ChainParam;
import io.taucoin.processor.StateProcessor;
import io.taucoin.processor.StateProcessorImpl;
import io.taucoin.torrent.DHT;
import io.taucoin.torrent.TorrentDHTEngine;
import io.taucoin.types.TypesConfig;
import io.taucoin.types.GenesisTx;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.Repo;
import io.taucoin.types.Block;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.util.Collections;
import java.util.*;

/**
 * ChainManager manages all blockchains for tau multi-chain system.
 * It's responsible for creating chain, following chain, unfollow chain,
 * pause chain, resume chain and so on.
 */
public class ChainManager {

    private static final Logger logger = LoggerFactory.getLogger("ChainManager");

    // Chain maps: chainID -> Chain.
    private Map<ByteArrayWrapper, Chain> chains
            = Collections.synchronizedMap(new HashMap<>());

    private TauListener listener;

    // state database
    private StateDBImpl stateDB;

    // state path
    private static final String STATE_PATH = "state";

    // block database
    private BlockDB blockDB;

    // block store path
    private static final String BLOCK_PATH = "block";

    /**
     * ChainManager constructor.
     *
     * @param listener CompositeTauListener
     */
    public ChainManager(TauListener listener, KeyValueDataBaseFactory dbFactory) {
        this.listener = listener;

        // create state and block database.
        // If database does not exist, directly load.
        // If not exist, create log
        this.stateDB = new StateDBImpl(dbFactory.newDatabase());
        this.blockDB = new BlockDB(dbFactory.newDatabase());
    }

    public void openChainDB() throws Exception {
        try {
            this.stateDB.open(Repo.getRepoPath() + File.separator + STATE_PATH);
            this.blockDB.open(Repo.getRepoPath() + File.separator + BLOCK_PATH);
        } catch (Exception e) {
            throw e;
        }
    }

    public boolean initTauChain() throws Exception {
		// New TauConfig
		GenesisConfig tauConfig = TauGenesisConfig.getInstance();
		byte[] chainid = TauGenesisTransaction.ChainID;
		logger.info("Initial tau chain chainid: {}", new String(chainid));

		if(!this.stateDB.isChainFollowed(chainid)) {
			// New TauChain
    		if (!createNewCommunity(tauConfig)) {
    		    return false;
            }
		}

		return true;
    }

    public void closeChainDB() {
        this.stateDB.close();
        this.blockDB.close();
    }

    /**
     * Start all followed and mined blockchains.
     * This method is called by TauController.
     */
    public void start() {

		// Open the db for repo and block
        try {

            openChainDB();
			
			initTauChain();

            Set<byte[]> followedChains = new HashSet<byte[]>();
            followedChains= this.stateDB.getAllFollowedChains();

            Iterator<byte[]> chainsItor = followedChains.iterator();
            while (chainsItor.hasNext()) {

                // New single chain
                byte[] chainid = chainsItor.next();
                ByteArrayWrapper wrapperChainID = new ByteArrayWrapper(chainid);

                logger.info("Follow chain ID: {}", new String(chainid));
                Chain chain = new Chain(chainid, this.blockDB, this.stateDB, this.listener);

                // Add chain
                this.chains.put(wrapperChainID, chain);
                // start chain process
                chain.start();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            // notify starting blockchains exception.
            listener.onChainManagerStarted(false, e.getMessage());
            return;
        }

        listener.onChainManagerStarted(true, "");
    }

    /**
     * Stop all followed and mined blockchains.
     * This method is called by TauController.
     */
    public void stop() {
		// stop chains
        Iterator<ByteArrayWrapper> chainsItor = this.chains.keySet().iterator();
        while(chainsItor.hasNext()) {
			chains.get(chainsItor.next()).stop();	
		}

		// close db
		closeChainDB();

        listener.onChainManagerStopped();
    }

    /**
     * Follow some chain specified by chain id.
     *
     * @param chainID
     * @return boolean successful or not.
     */
    public boolean followChain(byte[] chainID) {
		try{
			this.stateDB.followChain(chainID);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
        return true;
    }

    /**
     * Unfollow some chain specified by chain id.
     *
     * @param chainID
     * @return boolean successful or not.
     */
    public boolean unfollowChain(byte[] chainID) {
        return false;
    }

    /**
     * update public key
     * @param pubKey
     */
    public void updateKey(byte[] pubKey) {
        for (Map.Entry<ByteArrayWrapper, Chain> entry: this.chains.entrySet()) {
            entry.getValue().getTransactionPool().updatePubKey(pubKey);
        }
    }

    /**
     * get chain by chain ID
     * @param chainID chain ID
     * @return chain or null if not found
     */
    public Chain getChain(byte[] chainID) {
        return chains.get(new ByteArrayWrapper(chainID));
    }

    /**
     * Create new community.
     *
     * @param communityName community name
     * @param genesisItems airdrop accounts balance and power
     * @return boolean true indicates creating successfully, or else false.
     */
    public boolean createNewCommunity(String communityName,
            HashMap<ByteArrayWrapper, GenesisItem> genesisItems) {

        GenesisConfig config = new GenesisConfig(this, communityName, genesisItems);
        return createNewCommunity(config);
    }

    /**
     * create new community.
     * @param cf
     * @return true/false.
     */
    public boolean createNewCommunity(GenesisConfig cf){
        byte[] chainID = cf.getChainID();
		
        Block genesis = cf.getBlock();
        BlockContainer genesisContainer = new BlockContainer(genesis, cf.getTransaction());

        boolean ret = followChain(chainID);

        // load genesis state
        if (!loadGenesisState(chainID, genesisContainer)) {
            return false;
        }

        Transaction tx = genesisContainer.getTx();

        if (null == tx) {
            logger.error("Genesis tx is null!");
            return false;
        }

        if (tx.getTxType() != TypesConfig.TxType.GenesisType.ordinal()) {
            logger.error("Genesis type mismatch!");
            return false;
        }

        HashMap<ByteArrayWrapper, GenesisItem> map = ((GenesisTx) tx).getGenesisAccounts();
        if (null == map || map.size() <= 0) {
            logger.error("Genesis account is empty.");
            return false;
        }

        // add peer and put block to dht
        for (Map.Entry<ByteArrayWrapper, GenesisItem> entry : map.entrySet()) {
            byte[] pubKey = entry.getKey().getData();
			logger.info("create new community pubkey: {}", Hex.toHexString(pubKey));
            try {
                this.stateDB.addPeer(chainID, pubKey);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return false;
            }
            putBlockToDHT(chainID, genesisContainer, pubKey);
        }


		if(ret) {
        	try {
        	    logger.info("Save genesis block in block store. Chain ID:{}",
                        new String(chainID));
            	blockDB.saveBlock(chainID, genesis,true);
        	} catch (Exception e) {
            	logger.error(e.getMessage(), e);
				return false;
        	}
		}

		return true;
    }

    /**
     * load genesis state to db
     * @param chainID chain id
     * @param genesisContainer genesis block container
     * @return
     */
    private boolean loadGenesisState(byte[] chainID, BlockContainer genesisContainer) {
        try {
            StateProcessor stateProcessor = new StateProcessorImpl(chainID);
            StateDB track = stateDB.startTracking(chainID);
            if (stateProcessor.backwardProcessGenesisBlock(genesisContainer, track)) {
                track.setBestBlockHash(chainID, genesisContainer.getBlock().getBlockHash());
                track.setSyncBlockHash(chainID, genesisContainer.getBlock().getBlockHash());
                track.commit();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * put block to dht
     * @param chainID chain id
     * @param blockContainer block to put
     * @param peer hash link
     */
    private void putBlockToDHT(byte[] chainID, BlockContainer blockContainer, byte[] peer) {
        if (null != blockContainer) {
            if (null != blockContainer.getTx()) {
                // put immutable block
                DHT.ImmutableItem immutableItem =
                        new DHT.ImmutableItem(blockContainer.getTx().getEncoded());
                TorrentDHTEngine.getInstance().dhtPut(immutableItem);
            }

            // put immutable block first
            DHT.ImmutableItem immutableItem = new DHT.ImmutableItem(blockContainer.getBlock().getEncoded());
            TorrentDHTEngine.getInstance().dhtPut(immutableItem);

            MutableItemValue mutableItemValue = new MutableItemValue(blockContainer.getBlock().getBlockHash(), peer);

            // put mutable item
            byte[] blockSalt = makeBlockSalt(chainID);
            Pair<byte[], byte[]> keyPair = AccountManager.getInstance().getKeyPair();
            DHT.MutableItem mutableItem = new DHT.MutableItem(keyPair.first, keyPair.second,
                    mutableItemValue.getEncoded(), blockSalt);
            TorrentDHTEngine.getInstance().dhtPut(mutableItem);
        }
    }

    /**
     * make block salt
     * @return
     */
    private byte[] makeBlockSalt(byte[] chainID) {
        byte[] salt = new byte[chainID.length + ChainParam.BLOCK_CHANNEL.length];
        System.arraycopy(chainID, 0, salt, 0, chainID.length);
        System.arraycopy(ChainParam.BLOCK_CHANNEL, 0, salt, chainID.length,
                ChainParam.BLOCK_CHANNEL.length);
        return salt;
    }

    /**
     * according to chainid select a chain to sendTransaction.
     * @param tx
     * @return
     * todo
     */
    public void sendTransaction(Transaction tx){
		// get chainid
		ByteArrayWrapper chainid= new ByteArrayWrapper(tx.getChainID());

		// get chain and then add tx into the txpool
		chains.get(chainid).getTransactionPool().addTx(tx);
    }

    /**
     * get account state according to chainid and pubkey
     * @param chainid
     * @param pubkey
     * @return
     * todo
     */
    public AccountState getAccountState(byte[] chainid, byte[] pubkey) throws Exception {

		AccountState account= null;

		try{
			account= this.stateDB.getAccount(chainid, pubkey);
        } catch (Exception e) {
            throw e;
        }

		return account;
    }

    /**
     * get best block
     * @return
     * todo
     */
    public ArrayList<Block> getAllBestBlocks(){

		ArrayList<Block> blocks= new ArrayList<Block>();

		for (Map.Entry<ByteArrayWrapper, Chain> entry: this.chains.entrySet()) {
		    logger.debug("Chain ID: {}", new String(entry.getKey().getData()));
		    blocks.add(entry.getValue().getBestBlockContainer().getBlock());
        }

		return blocks;
    }

    /**
     * make all followed chain start mining.
     * @return
     * todo
     */
    public byte[][] startMining(){
        return null;
    }
}
