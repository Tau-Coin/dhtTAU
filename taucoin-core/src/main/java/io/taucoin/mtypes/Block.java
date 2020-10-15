/**
Copyright 2020 taucoin developer

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
(the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
OR OTHER DEALINGS IN THE SOFTWARE.
*/
package io.taucoin.mtypes;

import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Block {
        
    private static final Logger logger = LoggerFactory.getLogger("Block");

    // Block字段
    private long version;
    private long timestamp;
    private long blockNum;

    private byte[] previousBlockHash;    //Hash - 20 Bytes
    private byte[] immutableBlockHash;   //Hash - 20 Bytes
    private BigInteger baseTarget;
    private BigInteger cumulativeDifficulty;
    private byte[] generationSignature;  //Hash - 20 Bytes
    private byte[] txHash;     //Hash - 20 Bytes

    private BigInteger minerBalance;
    private BigInteger senderBalance;
    private BigInteger receiverBalance;
    private BigInteger senderNonce;

    private byte[] signature;     //Signature - 64 Bytes
    private byte[] minerPubkey;   //Pubkey - 32 Bytes

    // 中间结果，暂存内存，不上链
    private byte[] encodedBytes;
    private byte[] sigEncodedBytes;
    private byte[] blockHash;
    private boolean isParsed;
}
