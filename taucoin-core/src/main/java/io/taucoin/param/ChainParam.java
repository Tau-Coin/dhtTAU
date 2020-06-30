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

import com.frostwire.jlibtorrent.Ed25519;

import java.math.BigInteger;

import io.taucoin.genesis.GenesisMsg;

/**
 * chain config parameters.
 */
public class ChainParam {
    public static final String TauGenesisMinerPubkey="4294f0c9081ae86417d547774fc4c61c29468ea6298a1165644d323c45fb2e6d";
    public static final String TauGenerationSignature="168886";
    public static final String TauGenesisSignature="988066f393d83e938744a68b82b306cfcdd6d36ad38060b747e5e61e4d08824f991ac5176df83482aa8979553bc4d074e412797d04b6867efc147895efc68e28";
    public static final String TauGenesisMsg="f901449190746175636f696e20636861696e2e2e2eb897b89535386238663937396135313164633365623161343261323338383139383838383030313666663436333338666363633862343633373936393533373765643639626133393833383063336433643665306133653365393262323361353063326136633034663366383732343932636232313662653834306138663730663130303a6339383730316336626635323633343030303830b897b89564306463313565316636653366323130396430383966313165303837393034373263643961333961636565663664663435373533633362356265653963313437363439633832646561623739323134393262356435313031323433313865393433343632656564343763616330613939383235643835366165623433343962333a6339383730316336626635323633343030303830";
    public static final long TauGenesisTimeStamp=15000000;
    //default block time interval.
    public static final int DefaultBlockTimeInterval=300;
    public static final byte DefaultGenesisVersion = 0x01;
    public static final String TauCommunityName="TAUcoin";
    public static final String ChainidDelimeter="#";
    public static final int AnnounceLength = 256;
    public static final int AttachLength = 256;
    public static final int DescriptionLength = 256;
    public static final int MaxGenesisMsgItems = 5;
    public static final int HashLength = 32;
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
}
