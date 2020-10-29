/**
 * Copyright 2020 taucoin developer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
 * SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.taucoin.util;

import com.frostwire.jlibtorrent.swig.sha1_hash;
import com.frostwire.jlibtorrent.Vectors;
import com.frostwire.jlibtorrent.swig.byte_vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * hash utils used to calculate bytes length at least more than 20 bytes
 * warming:
 * com.frostwire.jlibtorrent.Sha1Hash
 */
public class HashUtil {
    private static final Logger logger = LoggerFactory.getLogger("HashUtil");

    public static byte[] sha1hash(byte[] bytes){
       MessageDigest digest;
       try {
           digest = MessageDigest.getInstance("SHA-1");
       } catch (NoSuchAlgorithmException e) {
           return null;
       }
       byte_vector bvs = Vectors.bytes2byte_vector(digest.digest(bytes));
       sha1_hash hash = new sha1_hash(bvs);
       return  Vectors.byte_vector2bytes(hash.to_bytes());
    }

    public static byte[] bencodeHash(byte[] bytes){

        String bencodeLen = bytes.length + ":";
        byte[] prefix = null;
        try {
            prefix = bencodeLen.getBytes("ASCII");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.toString());
        }

        byte[] getPrefixBytes = new byte[prefix.length + bytes.length];
        System.arraycopy(prefix, 0, getPrefixBytes, 0, prefix.length);
        System.arraycopy(bytes, 0, getPrefixBytes, prefix.length, bytes.length);

        return HashUtil.sha1hash(getPrefixBytes);

    }
}
