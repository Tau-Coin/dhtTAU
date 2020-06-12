## Original TAU Address

### generation procedure
Taucoin address generation is similar to Bitcoin
```
1. Random(LuckyBoy) -> TAUPrivateKey-256 Bits, 32 Bytes

2. ECCSecpk256(TAUPrivateKey) -> TAUPublicKey-3 Types(04+ 64Bytes, 02||03+32Bytes)

3. Ripemd160(Sha256(TAUPublicKey)) -> TAUAddTemp-20 Bytes

4. DefinedNetworkNo-1 Byte+ TAUAddTemp -> DefinedAddTemp-21 Bytes

5. Sha256(Sha256(DefinedAddTemp)) -> HashDefinedAddTemp-32Bytes

6. Get 4 Bytes In HashDefinedAddTemp End-> HDAT[0], HDAT[1], HDAT[2], HDAT[3]

7. DefinedAddTemp+HDAT[3-0] -> FinalTAUAdd-25 Bytes

8. Base58encode(FinalTAUAdd) -> TaucoinFinalAddress-34 Bytes
```

## Account In Tau-Torrent Publishing System

### ed25519
[Details seen in libtorrent.org](http://libtorrent.org/reference-ed25519.html)

- ed25519 seed (32 bytes)
- ed25519 private key (64 bytes)
- ed25519 public key (32 bytes)

在TPS中，只有私钥和公钥概念，抛弃使用地址系统。

1. Mining过程中节点的选择，交易信息的字段，都基于PublicKey进行；

2. Tau链中的GenesisBlock，持币也是基于ed25519的公钥，该公钥生成过程：

```
	2.1 升级原有Tau APK，基于Tau APK中的私钥作为ed25519中的seed，形成ed25519下的公私钥系统；

	2.2 上传信息(原有的地址+ed25519公钥)至后台，利用收集到的公钥进行GenesisBlock的持币；	

	2.3 第一版中县利用交易来进行airdrop；
```
