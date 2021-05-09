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
package io.taucoin.param;

import java.math.BigInteger;

/**
 * chain config parameters.
 */
public class ChainParam {
    public static final String TauGenesisMinerPubkey="3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124";
    public static final String TauGenerationSignature="9f0c546cceb1932f01d3fe99029038663fe75f2e65377f88093ae2c30ee31f27";
    public static final String TauGenesisSignature="6804fadd97e23349d95438df94eba47d8abdc76e4d4196cdb0d5fc65ed7ed11cb6c3e8e9eda4a95a7baec0c79a01c19233082d14583fb387712ad8b9a3cedd04";
    public static final String TauGenesisMsg="f9010992636f6d6d756e69747920636861696e2e2e2ef850b840383039646635313865653435306465643061363539616562346263356265633633366532636666303132666338386433343362373431396166393734626238318dcc8700e35fa931a00083019aa0f850b840326136323836383237316633643334353565346231656130633166393632363337333264303334373334396639646161333234373130376365316232623266398dcc8700e35fa931a00083019aa0f850b840336538376333356432303739383538643838646362313133656461646166316233333966636434663734633533396661613961396264353965373837663132348dcc8700e35fa931a00083019aa0";
    public static final long TauGenesisTimeStamp=1596554530;
    public static final byte DefaultGenesisVersion = 0x01;
    public static final String TauCommunityName="TAUcoin";
    public static final String ChainidDelimeter="#";
    public static final int HashLength = 20;
    public static final int SenderLength = 32;
    public static final int SignatureLength = 64;
    public static final int PubkeyLength = 32;
    public static final int BlockTimeDrift = 300;
    //max block size 1000 bytes.
    public static final int MaxBlockSize = 1000;
    //max total supply every chain
    public static final long MaxTotalSupply = 1000000000000000L;
    public static final BigInteger MaxBaseTarget = new BigInteger("ffffffffffffffff",16);
    public static final BigInteger MaxCummulativeDiff = new BigInteger("ffffffffffffffff",16);
    //default genesis power equal to power/year.
    public static final BigInteger DefaultGeneisisPower = new BigInteger("105120");

    // MAX ITEM SIZE IN ONE PUT
    public static final int MAX_DHT_PUT_ITEM_SIZE = 8;

    // time
    public static final int DEFAULT_BLOCK_TIME = 300;
    public static final int DEFAULT_MIN_BLOCK_TIME = 60;
    public static final int DEFAULT_MAX_BLOCK_TIME = 540;

    // range
    public static final int MUTABLE_RANGE = 288; // 1 days
    public static final int WARNING_RANGE = MUTABLE_RANGE * 3;

    // 一般public key截取保留的长度
    public static final int SHORT_ADDRESS_LENGTH = 4;

    // device id限制长度
    public static final int DEVICE_ID_LIMIT_SIZE = 4;

    public static final int MAX_HASH_NUMBER = 47;

    public static final int DHT_ITEM_LIMIT_SIZE = 996; // 996 byte

    public static final int MESSAGE_LIST_SAFE_SIZE = 960;

    public static final int GOSSIP_ITEM_LIMIT_SIZE = 58;

    public static final int INDEX_HASH_LIMIT_SIZE1 = 46;

    // 同步消息列表的最大数量
    public static final int MAX_MESSAGE_LIST_SIZE = 50;

    public static final int MAX_FRIEND_LIST_SIZE = 8;

    // transaction pool
    public static final int SLIM_DOWN_SIZE = WARNING_RANGE;

    public static final int TX_GOSSIP_SIZE = 3;

    /**********CHANNEL SALT**********/
    // tip
    public static final byte[] TIP_CHANNEL = "#T".getBytes();

    // demand
    public static final byte[] DEMAND_CHANNEL = "#D".getBytes();

    // gossip
    public static final byte[] GOSSIP_CHANNEL = "GOSSIP_TEST3".getBytes();

    // group chat gossip channel prefix
    public static final byte[] GROUP_CHAT_GOSSIP_CHANNEL_PREFIX = "GC_".getBytes();

    // one day
    public static final int ONE_DAY = 24 * 60 * 60;

    public static final int HashLongArrayLength = 3;

    public static final int PubkeyLongArrayLength = 4;

    public static final int SignLongArrayLength = 8;
}
