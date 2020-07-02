# Block 
 No              |  Key           | Size-Byte        |  Notes
 ----------------|----------------|------------------|----------------------
 1   |version        | 1          | "0x1" as initial default, increase block through version 
 2   |chainID        | 32         | Community ChainID := community name#coins volumn in millions#optional block time interval in seconds#hash(GenesisMInerPubkey + timestamp)
 3   |timestamp      | 4          | unix timestamp for winning the block package right
 4   |blockNumber    | 8          | block number
 5   |previousBlockHash  | 32     | hash of previous block
 6   |immutableBlockHash | 32     | hash of immutable point block
 7   |basetarget     | 8          |  for POT - Proof of Transaction calculation
 8   |cumulativedifficulty  | 8   | current consensus chain parameter
 9   |generationsignature  | 32  | for POT calculation, $9 x power x time
 10  |txMsg          | 32         | packaged transaction in block, One block only have one transaction
 11  |minerBalance    | 4         | miner's balance
 12  |senderBalance  | 4         | sender's balance
 13  |receiverBalance| 4         | receiver's balance
 14  |senderNonce      | 8       | sender's nonce, == power
 15  |signature      | 65         | r: 32 bytes, s: 32 bytes, v: 1 byte, when at #6 same difficulty, high signature number wins.


# Transaction
 No              |  Key           | Size-Byte        |  Notes
 ----------------|----------------|------------------|----------------------
 1   |version     | 1          | "0x1" as initial default, increase block through version 
 2   |chainID     | 32         | Community ChainID := community name#coins volumn in millions#optional block time interval in seconds#hash(GenesisMInerPubkey + timestamp)
 3   |timestamp   | 4          | unix timestamp for winning the block package right
 4   |expiredTime | 2          | block number
 5   |txFee         | 4          | root hash of previous block
 6   |sender      | 32         | root hash of immutable point block
 7   |nonce       | 8          | for POT - Proof of Transaction calculation
 8   |txData      | 32         | packaged transaction in block, One block only have one transaction
 9   |signature   | 65         | r: 32 bytes, s: 32 bytes, v: 1 byte, when at #6 same difficulty, high signature number wins.

## TxData
8 - txdata class

 No|  Key   |type    | Size-Byte|  Notes
 --|--------|--------|----------|----------------------
 1 | msgType|enum    | 1        | 0-regularforum, 1-forumcomment, 2-CommunityAnnouncement, 3-DHTbootstrapNodeAnnouncement,4-wiringTransaction,5-IdentityAnnouncement
 2 | txCode |byte[]  |  512     | Contract description code.
## txCode
8.1 -RegularForum
No |  Key    |type   | Size-Byte |  Notes
---|---------|-------|-----------|----------------------
 1 | forumMsg|string |  512      | forum msg

8.2 -ForumComment
No |  Key     |type   | Size-Byte |  Notes
---|----------|-------|-----------|----------------------
 1 | Reference|byte[] |  32       | reference block hash
 2 | Comment  |String | 256       | comments msg

8.3 -CommunityAnnouncement
No |  Key           |type   | Size-Byte |  Notes
---|----------------|-------|-----------|----------------------
 1 | ChainID        |byte[] |  64       | CommunityChainid
 2 | GenesisPubkey  |byte[] |  32       | GenesisMiner pubkey

8.4 -DHTbootstrapNodeAnnouncement
No |  Key           |type     | Size-Byte  |  Notes
---|----------------|---------|------------|----------------------
 1 | ChainID        |byte[]   |  64        | CommunityChainid
 2 | BootNodes      |string[] |  256       | Node network address

8.5 -WiringTransaction
No |  Key           |type     | Size-Byte  |  Notes
---|----------------|---------|------------|----------------------
 1 | ReceiverPubkey |byte[]   |  32        | receiver Pubkey
 2 | Amount         |long     |  8         | wire amount

8.6 -IdentityAnnouncement
No |  Key           |type     | Size-Byte  |  Notes
---|----------------|---------|------------|----------------------
 1 | RenamePubkey   |byte[]   |  32        | associate Pubkey
 2 | NewName        |String   |  256       | new name
