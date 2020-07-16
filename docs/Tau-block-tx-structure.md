# Block 
- [ ]  No              |  Key           | Size-Byte        | Notes | encoded    bytes 
   ----------------|----------------|------------------|----------------------|----------------------
   1   |version        | 1          | "0x1" as initial default, increase block through version(less than 127) | 2 
   2   |chainID        | 64       | Community ChainID := community name#optional block time interval in seconds#hash(GenesisMInerPubkey + timestamp) | 66 
   3   |timestamp      | 8         | unix timestamp for winning the block package right| 9 
   4   |blockNumber    | 8          | block number| 9 
   5   |previousBlockHash  | 32     | hash of previous block| 33 
   6   |immutableBlockHash | 32     | hash of immutable point block| 33 
   7   |baseTarget     | 8          |  for POT - Proof of Transaction calculation| 9 
   8   |cumulativeDifficulty  | 8   | current consensus chain parameter| 9 
   9   |generationSignature  | 32  | for POT calculation, $9 x power x time| 33 
   10  |txMsg          | **less than 559** | packaged transaction in block, One block only have one transaction| **less than 662** 
   11  |minerBalance    | 8        | miner's balance| 9 
   12  |senderBalance  | 8        | sender's balance| 9 
   13  |receiverBalance| 8        | receiver's balance| 9 
   14  |senderNonce      | 8       | sender's nonce, == power| 9 
   15  |signature      | 64         | r: 32 bytes, s: 32 bytes, when at #6 same difficulty, high signature number wins.| 66 
   16  |minerPk        | 32         | miner public key| 33 




# Transaction
 No              |  Key           | Size-Byte        |  Notes| encoded bytes 
 ----------------|----------------|------------------|----------------------|----------------------
 1   |version     | 1          | "0x1" as initial default, increase block through version | 2 
 2   |chainID     | 64       | Community ChainID := community name#optional block time interval in seconds#hash(GenesisMInerPubkey + timestamp) | 66 
 3   |timestamp   | 8         | unix timestamp for winning the block package right| 9 
 4   |txFee         | 8         | tx fee| 9 
 5   |senderPk      | 32         | sender public key| 33 
 6   |nonce       | 8          | for POT - Proof of Transaction calculation| 9 
 7   |txData      | **less than 359** | packaged transaction in block, One block only have one transaction| **less than 362** 
 8   |signature   | 64         | r: 32 bytes, s: 32 bytes, when at #6 same difficulty, high signature number wins.| 66 

## TxData
8 - txdata class

 No|  Key   |type    | Size-Byte|  Notes| encoded bytes 
 --|--------|--------|----------|----------------------|----------------------
 1 | msgType|enum    | 1        | 0-regularforum, 1-forumcomment, 2-CommunityAnnouncement, 3-DHTbootstrapNodeAnnouncement,4-wiringTransaction,5-IdentityAnnouncement| 2 
 2 | txCode |byte[]  |  **less than 351**  | Contract description code.| **less than 354** 
## txCode
8.1 -Note

No |  Key    |type   | Size-Byte |  Notes| encode bytes 
---|---------|-------|-----------|----------------------|----------------------
 1 | forumMsg|string |  351  | forum msg| **less than 351** 

8.2 -Comment

No |  Key     |type   | Size-Byte |  Notes| encode bytes 
---|----------|-------|-----------|----------------------|----------------------
 1 | reference|byte[] |  20     | reference transaction hash | 21 
 2 | comment  |String | 324 | comments msg| **less than 327** 

8.3 -CommunityAnnouncement

No |  Key           |type   | Size-Byte |  Notes| encode bytes 
---|----------------|-------|-----------|----------------------|----------------------
 1 | annChainID    |byte[] |  64       | communityChainID| 66 
 2 | bootstrapPks |byte[] |  32       | genesisMiner pubkey| 33 
 3 | description |String | 247 | community description | **less than 249** 

8.4 -DHTbootstrapNodesAnnouncement-**deprecated**

No |  Key           |type     | Size-Byte  |  Notes
---|----------------|---------|------------|----------------------
 1 | bootstrapNodes |string[] |  256       | node network address 

8.5 -WiringTransaction

No |  Key           |type     | Size-Byte  |  Notes| encode bytes 
---|----------------|---------|------------|----------------------|----------------------
 1 | receiverPk |byte[]   |  32        | receiver Pubkey| 33 
 2 | amount         |long     |  8         | wire amount| 9 
 3 | notes |String | 303 | explanation of this transaction | **less than 306** 

8.6 -IdentityAnnouncement

No |  Key           |type     | Size-Byte  | Notes | encode bytes 
---|----------------|---------|------------|----------------------|----------------------
 1 | name       |String   |  63     | new name| **define 64** 
 2 | description |String | 281 | social media account | **less than 284** 
