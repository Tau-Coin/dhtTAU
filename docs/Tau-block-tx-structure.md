# Block 
 No              |  Key           | Size-Byte        |  Notes
 ----------------|----------------|------------------|----------------------
 1   |version        | 1          | "0x1" as initial default, increase block through version 
 2   |chainID        | 32         | Community ChainID := community name#coins volumn in millions#optional block time interval in seconds#hash(GenesisMInerPubkey + timestamp)
 3   |timestamp      | 4          | unix timestamp for winning the block package right
 4   |blockNumber    | 8          | block number
 5   |previousBlockHash  | 32     | hash of previous block
 6   |immutableBlockHash | 32     | hash of immutable point block
 7   |baseTarget     | 8          |  for POT - Proof of Transaction calculation
 8   |cumulativeDifficulty  | 8   | current consensus chain parameter
 9   |generationSignature  | 32  | for POT calculation, $9 x power x time
 10  |txMsg          | 32         | packaged transaction in block, One block only have one transaction
 11  |minerBalance    | 4         | miner's balance
 12  |senderBalance  | 4         | sender's balance
 13  |receiverBalance| 4         | receiver's balance
 14  |senderNonce      | 8       | sender's nonce, == power
 15  |signature      | 64         | r: 32 bytes, s: 32 bytes, when at #6 same difficulty, high signature number wins.
 16  |publicKey        | 32         | miner public key

# Transaction
 No              |  Key           | Size-Byte        |  Notes
 ----------------|----------------|------------------|----------------------
 1   |version     | 1          | "0x1" as initial default, increase block through version 
 2   |chainID     | 32         | Community ChainID := community name#coins volumn in millions#optional block time interval in seconds#hash(GenesisMInerPubkey + timestamp)
 3   |timestamp   | 4          | unix timestamp for winning the block package right
 4   |txFee         | 4          | tx fee
 5   |sender      | 32         | sender public key
 6   |nonce       | 8          | for POT - Proof of Transaction calculation
 7   |txData      | 32         | packaged transaction in block, One block only have one transaction
 8   |signature   | 64         | r: 32 bytes, s: 32 bytes, when at #6 same difficulty, high signature number wins.

## TxData
8 - txdata class

 No|  Key   |type    | Size-Byte|  Notes
 --|--------|--------|----------|----------------------
 1 | msgType|enum    | 1        | 0-regularforum, 1-forumcomment, 2-CommunityAnnouncement, 3-DHTbootstrapNodeAnnouncement,4-wiringTransaction,5-IdentityAnnouncement
 2 | txCode |byte[]  |  512     | Contract description code.
## txCode
8.1 -Note

No |  Key    |type   | Size-Byte |  Notes
---|---------|-------|-----------|----------------------
 1 | forumMsg|string |  512      | forum msg

8.2 -Comment

No |  Key     |type   | Size-Byte |  Notes
---|----------|-------|-----------|----------------------
 1 | reference|byte[] |  32       | reference block hash
 2 | comment  |String | 256       | comments msg

8.3 -CommunityAnnouncement
No |  Key           |type   | Size-Byte |  Notes
---|----------------|-------|-----------|----------------------
 1 | chainID        |byte[] |  64       | communityChainID
 2 | genesisPubkey  |byte[] |  32       | genesisMiner pubkey
 3 | description |String | 256 | community description 

8.4 -DHTbootstrapNodeAnnouncement

No |  Key           |type     | Size-Byte  |  Notes
---|----------------|---------|------------|----------------------
 1 | chainID        |byte[]   |  64        | communityChainID
 2 | bootNodes      |string[] |  256       | node network address

8.5 -WiringTransaction
No |  Key           |type     | Size-Byte  |  Notes
---|----------------|---------|------------|----------------------
 1 | receiverPubkey |byte[]   |  32        | receiver Pubkey
 2 | amount         |long     |  8         | wire amount
 3 | notes |String | 256 | explanation of this transaction 

8.6 -IdentityAnnouncement
No |  Key           |type     | Size-Byte  |  Notes
---|----------------|---------|------------|----------------------
 1 | renamePubkey   |byte[]   |  32        | associate Pubkey
 2 | newName        |String   |  256       | new name
