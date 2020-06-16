# Block 
 No              |  Key           | Size-Byte        |  Notes
 ----------------|----------------|------------------|----------------------
 1   |version        | 1          | "0x1" as initial default, increase block through version 
 2   |chainID        | 32         | Community ChainID := community name#coins volumn in millions#optional block time interval in seconds#hash(GenesisMInerPubkey + timestamp)
 3   |timestamp      | 4          | unix timestamp for winning the block package right
 4   |blockNumber    | 8          | block number
 5   |previousBlockRoot  | 32     | root hash of previous block
 6   |immutableBlockRoot | 32     | root hash of immutable point block
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

 No              |  Key           | Size-Byte        |  Notes
 ----------------|----------------|------------------|----------------------
 1 | msgType        | 1        | 0-torrent publishing, 1-wriring, 2-BootStrapNode Announcement, 3-Community Announcement
 2 | annoucement    | 1024     | Description of the magnet link, Receiver, BootStrapNode, Community Announcement
 3 | attachment     | 1024     | Magnet link, Amount, Nil of boostrapnoode and community announcement