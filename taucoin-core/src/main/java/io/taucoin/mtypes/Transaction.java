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

public abstract class Transaction {

    private static final Logger logger = LoggerFactory.getLogger("Transaction");

    // Transaction字段
    protected long version;
    protected byte[] chainID;

    protected long timestamp;
    protected BigInteger txFee;
    protected long txType;

    protected byte[] senderPubkey; //Pubkey - 32 bytes
    protected BigInteger nonce;
    protected byte[] signature;    //Signature - 64 bytes

    // 中间结果，暂存内存，不上链
    protected byte[] encodedBytes;
    protected byte[] sigEncodedBytes;
    protected byte[] txHash;
    protected boolean isParsed;

}
