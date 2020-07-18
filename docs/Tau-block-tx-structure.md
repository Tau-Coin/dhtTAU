# Block 
  No              |  Key           | Size-Byte        | Notes | RLP encoded size-byte 
  ---------------|----------------|------------------|:---------------------|----------------------
   1   |version        | 1          | "0x0" as initial default, increase block through version | 2 
   2   |chainID        | 64       | Community ChainID := community name#optional block time interval in seconds#hash(GenesisMInerPubkey + timestamp) | 66 
   3   |timestamp      | 8         | unix timestamp for winning the block package right| 9 
   4   |blockNumber    | 8          | block number| 9 
   5   |previousBlockHash  | 20   | hash of previous block| 21 
   6   |immutableBlockHash | 20   | hash of immutable point block| 21 
   7   |baseTarget     | 8          |  for POT - Proof of Transaction calculation| 9 
   8   |cumulativeDifficulty  | 8   | block chain difficulty parameter | 9 
   9   |generationSignature  | 32  | for POT calculation, $7 x power x time | 33 
   10  |txMsg          | **less than 683** | packaged transaction in block, One block only have one transaction| **less than 686** 
   11  |minerBalance    | 8        | miner's balance| 9 
   12  |senderBalance  | 8        | sender's balance| 9 
   13  |receiverBalance| 8        | receiver's balance| 9 
   14  |senderNonce      | 8       | sender's nonce(power) | 9 
   15  |signature      | 64         | r: 32 bytes, s: 32 bytes, when at #8 same difficulty, high signature number wins. | 66 
   16  |minerPk        | 32         | miner public key| 33 




# Transaction
 No              |  Key           | Size-Byte        |  Notes| RLP encoded size-byte 
 ----------------|----------------|------------------|----------------------|----------------------
 1   |version     | 1          | "0x0" as initial default, increase block through version | 2 
 2   |chainID     | 64       | Community ChainID := community name#optional block time interval in seconds#hash(GenesisMInerPubkey + timestamp) | 66 
 3   |timestamp   | 8         | unix timestamp for this transaction. | 9 
 4   |txFee         | 8         | tx fee| 9 
 5   |senderPk      | 32         | sender public key| 33 
 6   |nonce       | 8          | deny replicated tx | 9 
 7   |txData      | **less than 483** | packaged transaction data in a block. | **less than 486** 
 8   |signature   | 64         | r: 32 bytes, s: 32 bytes. | 66 

## TxData
8 - txdata class

 No|  Key   |type    | Size-Byte|  Notes| RLP encoded size-byte 
 --|--------|--------|----------|----------------------|----------------------
 1 | msgType|enum    | 1        | 0-regularforum, 1-forumcomment, 2-CommunityAnnouncement,3-wiringTransaction,4-IdentityAnnouncement, 5-DHTbootstrapNodeAnnouncement | 2 
 2 | txCode |byte[]  |  **less than 475**  | Contract description code.| **less than 478** 
## txCode
8.1 -Note

No |  Key    |type   | Size-Byte |  Notes| RLP encoded size-byte 
---|---------|-------|-----------|----------------------|----------------------
 1 | forumMsg|string |  475  | forum msg| **less than 475** 

8.2 -Comment

No |  Key     |type   | Size-Byte |  Notes| RLP encoded size-byte 
---|----------|-------|-----------|----------------------|----------------------
 1 | reference|byte[] |  20     | reference transaction hash | 21 
 2 | comment  |String | 448 | comments msg| **less than 451** 

8.3 -CommunityAnnouncement

No |  Key           |type   | Size-Byte |  Notes| RLP encoded size-byte 
---|----------------|-------|-----------|----------------------|----------------------
 1 | annChainID    |byte[] |  64       | communityChainID| 66 
 2 | bootstrapPk |byte[] |  32       | bootstrap pubkey | 33 
 3 | description |String | 370 | community description | **less than 373** 

8.4-WiringTransaction

No |  Key           |type     | Size-Byte  |  Notes| RLP encoded size-byte 
---|----------------|---------|------------|----------------------|----------------------
 1 | receiverPk |byte[]   |  32        | receiver Pubkey| 33 
 2 | amount         |long     |  8         | wire amount| 9 
 3 | notes |String | 427 | explanation of this transaction | **less than 430** 

8.5-IdentityAnnouncement

No |  Key           |type     | Size-Byte  | Notes | RLP encoded size-byte 
---|----------------|---------|------------|----------------------|----------------------
 1 | name       |String   |  63     | new name| **define 64** 
 2 | description |String | 405 | social media account | **less than 408** 

8.6-DHTbootstrapNodesAnnouncement-**deprecated**

| No   | Key            | type     | Size-Byte | Notes                |
| ---- | -------------- | -------- | --------- | -------------------- |
| 1    | bootstrapNodes | string[] | 256       | node network address |