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
    public static final String TauGenesisMinerPubkey="4294f0c9081ae86417d547774fc4c61c29468ea6298a1165644d323c45fb2e6d";
    public static final String TauGenerationSignature="9f0c546cceb1932f01d3fe99029038663fe75f2e65377f88093ae2c30ee31f27";
    public static final String TauGenesisSignature="988066f393d83e938744a68b82b306cfcdd6d36ad38060b747e5e61e4d08824f991ac5176df83482aa8979553bc4d074e412797d04b6867efc147895efc68e28";
    public static final String TauGenesisMsg="f9015b92636f6d6d756e69747920636861696e2e2e2ef850b840636137326661396331303035333134326331333032343235343132643666366163316230336562646332636263356663373637366431633331666464623665318dcc8700e35fa931a00083019aa0f850b840633739386663393161343033626366633664623230666662336331643966353935326566636238653039623430393733333863623739373439393361323130638dcc8700e35fa931a00083019aa0f850b840313864633465653933313637373066323631636131376364336262663331393330316139663039333062653361626366666262373365373361353064396662318dcc8700e35fa931a00083019aa0f850b840303930313339623430636466626334653830336630613532393366333030356161623531333265303838333231626166343463323736616433636634616264618dcc8700e35fa931a00083019aa0";
    public static final long TauGenesisTimeStamp=15000000;
    //default block time interval.
    public static final int DefaultBlockTimeInterval=300;
    public static final byte DefaultGenesisVersion = 0x01;
    public static final String TauCommunityName="TAUcoin";
    public static final String ChainidDelimeter="#";
    public static final int IDnameLength = 63;
    public static final int IDdescLength = 405;
    public static final int WireNoteLength = 427;
    public static final int ForumMsgLength = 475;
    public static final int CommunityDescriptionLength = 370;
    public static final int CommentLength = 448;
    public static final int DescriptionLength = 500;
    public static final int MaxGenesisMsgItems = 5;
    public static final int MaxBootNodesAnnouncement = 5;
    public static final int HashLength = 20;
    public static final int ChainIDlength = 64;
    public static final int SenderLength = 32;
    public static final int SignatureLength = 64;
    public static final int GenerationSigLength = 32;
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

    // range
    public static final int MUTABLE_RANGE = 864; // 3 days
    public static final int WARNING_RANGE = MUTABLE_RANGE * 3;

    // transaction pool
    public static final int SLIM_DOWN_SIZE = WARNING_RANGE;
}
