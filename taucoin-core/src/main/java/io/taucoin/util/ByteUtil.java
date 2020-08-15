package io.taucoin.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.spongycastle.util.encoders.Hex;

public class ByteUtil {

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final byte[] ZERO_BYTE_ARRAY = new byte[]{0};

    /**
     * Creates a copy of bytes and appends b to the end of it
     */
    public static byte[] appendByte(byte[] bytes, byte b) {
        byte[] result = Arrays.copyOf(bytes, bytes.length + 1);
        result[result.length - 1] = b;
        return result;
    }

    /**
     * The regular {@link BigInteger#toByteArray()} method isn't quite what we often need:
     * it appends a leading zero to indicate that the number is positive and may need padding.
     *
     * @param b the integer to format into a byte array
     * @param numBytes the desired size of the resulting byte array
     * @return numBytes byte long array.
     */
    public static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
        if (b == null)
            return null;
        byte[] bytes = new byte[numBytes];
        byte[] biBytes = b.toByteArray();
        int start = (biBytes.length == numBytes + 1) ? 1 : 0;
        int length = Math.min(biBytes.length, numBytes);
        System.arraycopy(biBytes, start, bytes, numBytes - length, length);
        return bytes;
    }

    /**
     * Omitting sign indication byte.
     * <br><br>
     * Instead of {org.spongycastle.util.BigIntegers#asUnsignedByteArray(BigInteger)}
     * <br>we use this custom method to avoid an empty array in case of BigInteger.ZERO
     *
     * @param value - any big integer number. A <code>null</code>-value will return <code>null</code>
     * @return A byte array without a leading zero byte if present in the signed encoding.
     *      BigInteger.ZERO will return an array with length 1 and byte-value 0.
     */
    public static byte[] bigIntegerToBytes(BigInteger value) {
        if (value == null)
            return null;

        byte[] data = value.toByteArray();

        if (data.length != 1 && data[0] == 0) {
            byte[] tmp = new byte[data.length - 1];
            System.arraycopy(data, 1, tmp, 0, tmp.length);
            data = tmp;
        }
        return data;
    }

    /**
     * Returns the amount of nibbles that match each other from 0 ...
     * amount will never be larger than smallest input
     *
     * @param a - first input
     * @param b - second input
     * @return Number of bytes that match
     */
    public static int matchingNibbleLength(byte[] a, byte[] b) {
        int i = 0;
        int length = a.length < b.length ? a.length : b.length;
        while (i < length) {
            if (a[i] != b[i])
                return i;
            i++;
        }
        return i;
    }

    /**
     * Converts a long value into a byte array.
     *
     * @param val - long value to convert
     * @return <code>byte[]</code> of length 8, representing the long value
     */
    public static byte[] longToBytes(long val) {
        return ByteBuffer.allocate(8).putLong(val).array();
    }

    /**
     * Converts a short value into a byte array.
     *
     * @param val - short value to convert
     * @return <code>byte[]</code> of length 2, representing the short value
     */
    public static byte[] shortToBytes(short val) {
        return ByteBuffer.allocate(2).putShort(val).array();
    }

    /**
     * Converts a long value into a byte array.
     *
     * @param val - long value to convert
     * @return decimal value with leading byte that are zeroes striped
     */
    public static byte[] longToBytesNoLeadZeroes(long val) {

        // todo: improve performance by while strip numbers until (long >> 8 == 0)
        byte[] data = ByteBuffer.allocate(8).putLong(val).array();

        return stripLeadingZeroes(data);
    }

    /**
     * Converts a long array list into a byte array.
     *
     * @param array - long arraylist to convert
     * @param byteLength - return length of bytes
     * @return decimal value with leading byte that are zeroes striped
     */
    public static byte[] longArrayToBytes(ArrayList<Long> longArray, int byteLength) {
        if (null == longArray) {
            return null;            
        }
        byte[] byteArray = new byte[byteLength];

        int count = 0;
        int mod = Constants.LongTypeLength;

        if (0 == byteLength % Constants.LongTypeLength) {
            count = byteLength / Constants.LongTypeLength;
        } else {
            count = byteLength / Constants.LongTypeLength + 1;
            mod = byteLength - (count - 1)* Constants.LongTypeLength;
        }

        for(int i = 0; i < count; i++) {

            byte[] bTemp = null;

            if (i == (count - 1)) {
                bTemp =  ByteUtil.keepNBytesOfLong(longArray.get(i), mod);
                System.arraycopy(bTemp, 0, byteArray, i * Constants.LongTypeLength, mod);
            } else {
                bTemp =  ByteUtil.longToBytes(longArray.get(i));
                System.arraycopy(bTemp, 0, byteArray, i * Constants.LongTypeLength, Constants.LongTypeLength);
            }

        }

        return byteArray;
    }

    /**
     * padding n bytes long to hash.
     * @param val
     * @param n
     * @return
     */
    public static byte[] keepNBytesOfLong(long val, int n){
        byte[] data = ByteBuffer.allocate(Constants.LongTypeLength).putLong(val).array();
        byte[] retval = new byte[n];
        System.arraycopy(data, Constants.LongTypeLength - n, retval, 0, n);
        return retval;
    }

    /**
     * padding 4 bytes to hash.
     * @param val
     * @return
     */
    public static byte[] keep4bytesOfLong(long val){
        byte[] data = ByteBuffer.allocate(8).putLong(val).array();
        byte[] retval = new byte[4];
        System.arraycopy(data, 4, retval, 0, 4);
        return retval;
    }

    public static byte[] intToBytes(int val){

        if (val == 0) return EMPTY_BYTE_ARRAY;

        int lenght = 0;

        int tmpVal = val;
        while (tmpVal != 0){
            tmpVal = tmpVal >> 8;
            ++lenght;
        }

        byte[] result = new byte[lenght];

        int index = result.length - 1;
        while(val != 0){

            result[index] = (byte)(val & 0xFF);
            val = val >> 8;
            index -= 1;
        }

        return result;
    }


    /**
     * Converts a int value into a byte array.
     *
     * @param val - int value to convert
     * @return value with leading byte that are zeroes striped
     */
    public static byte[] intToBytesNoLeadZeroes(int val){

        if (val == 0) return EMPTY_BYTE_ARRAY;

        int lenght = 0;

        int tmpVal = val;
        while (tmpVal != 0){
            tmpVal = tmpVal >>> 8;
            ++lenght;
        }

        byte[] result = new byte[lenght];

        int index = result.length - 1;
        while(val != 0){

            result[index] = (byte)(val & 0xFF);
            val = val >>> 8;
            index -= 1;
        }

        return result;
    }


    /**
     * Convert a byte-array into a hex String.<br>
     * Works similar to {@link Hex#toHexString}
     * but allows for <code>null</code>
     *
     * @param data - byte-array to convert to a hex-string
     * @return hex representation of the data.<br>
     *      Returns an empty String if the input is <code>null</code>
     *
     * @see Hex#toHexString
     */
    public static String toHexString(byte[] data) {
        return data == null ? "" : Hex.toHexString(data);
    }

    public static byte[] toByte(String str){
        return Hex.decode(str);
    }
    /**
     * Calculate packet length
     *
     * @param msg byte[]
     * @return byte-array with 4 elements
     */
    public static byte[] calcPacketLength(byte[] msg) {
        int msgLen = msg.length;
        return new byte[]{
                (byte) ((msgLen >> 24) & 0xFF),
                (byte) ((msgLen >> 16) & 0xFF),
                (byte) ((msgLen >> 8) & 0xFF),
                (byte) ((msgLen) & 0xFF)};
    }

    public static short byteArrayToShort(byte[] b) {
        if (b == null || b.length == 0)
            return 0;
        return new BigInteger(1, b).shortValue();
    }

    /**
     * Cast hex encoded value from byte[] to int
     *
     * Limited to Integer.MAX_VALUE: 2^32-1 (4 bytes)
     *
     * @param b array contains the values
     * @return unsigned positive int value.
     */
    public static int byteArrayToInt(byte[] b) {
        if (b == null || b.length == 0)
            return 0;
        return new BigInteger(1, b).intValue();
    }

    /**
     * Cast hex encoded value from byte[] to int
     *
     * Limited to Integer.MAX_VALUE: 2^32-1 (4 bytes)
     *
     * @param b array contains the values
     * @return unsigned positive long value.
     */
    public static long byteArrayToLong(byte[] b) {
        if (b == null || b.length == 0)
            return 0;
        return new BigInteger(1, b).longValue();
    }

    /**
     * express 8 bytes with a long num to save encoded storage.
     * @param b: 8 bytes will be transfered into long number.
     * @return negative , positive or 0 num
     */
    public static long byteArrayToSignLong(byte[] b){
        if (b.length != 8){
            throw new IllegalArgumentException("this function only accept 8 bytes argument");
        }

        if(b[0] == b[1] && b[1] == b[2] && b[2] == b[3] && b[3] == b[4] && b[4] == b[5] && b[5] == b[6]
        && b[6] == b[7] && b[7] == 0x00){
           return new BigInteger(0,b).longValue();
        }
        if(b[0] <= 0x7f){
           return new BigInteger(1,b).longValue();
        }
        if(b[0] > 0x7f){
           return new BigInteger(-1,b).longValue();
        }
        return 0;
    }

    public static int byteArrayToSignInt(byte[] b){
        if (b.length != 4){
            throw new IllegalArgumentException("this function only accept 4 bytes argument");
        }
        if(b[0] == b[1] && b[1] == b[2] && b[2] == b[3] && b[3] == 0x00){
            return new BigInteger(0,b).intValue();
        }
        if(b[0] <= 0x7f){
            return new BigInteger(1,b).intValue();
        }
        if(b[0] > 0x7f){
            return new BigInteger(-1,b).intValue();
        }
        return 0;
    }

    /**
     * slice bytes array into array consisting of long number.
     * @param b: byte array will be cut into piece num elements long array.
     * @param piece: element num of long array.
     * @return long array that express this bytes array b
     */
    public static ArrayList<Long> byteArrayToSignLongArray(byte[] b, int piece){
        if(b.length != 8*piece){
            throw new IllegalArgumentException("bytes array length must == "+8*piece);
        }
        ArrayList<Long> retval = new ArrayList<>();
        byte[] slice = new byte[8];
        for(int i = 0;i <piece;++i){
           System.arraycopy(b,8*i,slice,0,8);
           retval.add(byteArrayToSignLong(slice));
        }
        return retval;
    }

    public static int[] byteArrayToSignIntArray(byte[] b,int piece){
        if(b.length != 4*piece){
            throw new IllegalArgumentException("bytes array length must == "+4*piece);
        }
        int[] retval = new int[piece];
        byte[] slice = new byte[4];
        for(int i = 0;i <piece;++i){
            System.arraycopy(b,4*i,slice,0,4);
            retval[i] = byteArrayToSignInt(slice);
        }
        return retval;
    }

    /**
     * the last bytes piece in temp should be padding 0 before head byte.
     * @param b:[byte,byte,......,byte]
     * @param piece: b.length/8 +1
     * @return
     */
    public static ArrayList<Long> unAlignByteArrayToSignLongArray(byte[] b, int piece){
        byte[] temp = new byte[8 * piece];
        int alignCount = piece - 1;
        byte[] zero = new byte[8 * piece - b.length];

        for(int i = 0; i< zero.length; i++){
            zero[i] = 0x00;
        }
        System.arraycopy(b,0,temp,0,alignCount*8);
        System.arraycopy(zero,0,temp,alignCount*8,zero.length);
        System.arraycopy(b,alignCount*8,temp,alignCount*8 + zero.length,b.length-alignCount*8);
        return byteArrayToSignLongArray(temp,piece);
    }

    public static ArrayList<Long> stringToLongArrayList(String str){
        int start = str.indexOf("'");
        int end  = str.lastIndexOf("'");
        String newStr = str.substring(start, end + 1);
        String[] strArr = newStr.split(",");
        ArrayList<Long> ret = new ArrayList<>();
        for(int i = 0; i < strArr.length; i++) {
            ret.add(Long.valueOf(strArr[i].trim().replace("'","")));
        }
        return ret;
    }

    /**
     * Turn nibbles to a pretty looking output string
     *
     * Example. [ 1, 2, 3, 4, 5 ] becomes '\x11\x23\x45'
     *
     * @param nibbles - getting byte of data [ 04 ] and turning
     *                  it to a '\x04' representation
     * @return pretty string of nibbles
     */
    public static String nibblesToPrettyString(byte[] nibbles) {
        StringBuilder builder = new StringBuilder();
        for (byte nibble : nibbles) {
            final String nibbleString = oneByteToHexString(nibble);
            builder.append("\\x").append(nibbleString);
        }
        return builder.toString();
    }

    public static String oneByteToHexString(byte value) {
        String retVal = Integer.toString(value & 0xFF, 16);
        if (retVal.length() == 1) retVal = "0" + retVal;
        return retVal;
    }

    /**
     * Calculate the number of bytes need
     * to encode the number
     *
     * @param val - number
     * @return number of min bytes used to encode the number
     */
    public static int numBytes(String val) {

        BigInteger bInt = new BigInteger(val);
        int bytes = 0;

        while (!bInt.equals(BigInteger.ZERO)) {
            bInt = bInt.shiftRight(8);
            ++bytes;
        }
        if (bytes == 0) ++bytes;
        return bytes;
    }

    /**
     * @param arg - not more that 32 bits
     * @return - bytes of the value pad with complete to 32 zeroes
     */
    public static byte[] encodeValFor32Bits(Object arg) {

        byte[] data;

        // check if the string is numeric
        if (arg.toString().trim().matches("-?\\d+(\\.\\d+)?"))
            data = new BigInteger(arg.toString().trim()).toByteArray();
            // check if it's hex number
        else if (arg.toString().trim().matches("0[xX][0-9a-fA-F]+"))
            data = new BigInteger(arg.toString().trim().substring(2), 16).toByteArray();
        else
            data = arg.toString().trim().getBytes();


        if (data.length > 32)
            throw new RuntimeException("values can't be more than 32 byte");

        byte[] val = new byte[32];

        int j = 0;
        for (int i = data.length; i > 0; --i) {
            val[31 - j] = data[i - 1];
            ++j;
        }
        return val;
    }

    /**
     * encode the values and concatenate together
     *
     * @param args Object
     * @return byte[]
     */
    public static byte[] encodeDataList(Object... args) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Object arg : args) {
            byte[] val = encodeValFor32Bits(arg);
            try {
                baos.write(val);
            } catch (IOException e) {
                throw new Error("Happen something that should never happen ", e);
            }
        }
        return baos.toByteArray();
    }

    public static int firstNonZeroByte(byte[] data) {
        for (int i = 0; i < data.length; ++i) {
            if (data[i] != 0) {
                return i;
            }
        }
        return -1;
    }

    public static byte[] stripLeadingZeroes(byte[] data) {

        if (data == null)
            return null;

        final int firstNonZero = firstNonZeroByte(data);
        switch (firstNonZero) {
            case -1:
                return ZERO_BYTE_ARRAY;

            case 0:
                return data;

            default:
                byte[] result = new byte[data.length - firstNonZero];
                System.arraycopy(data, firstNonZero, result, 0, data.length - firstNonZero);

                return result;
        }
    }

    /**
     * increment byte array as a number until max is reached
     *
     * @param bytes byte[]
     * @return boolean
     */
    public static boolean increment(byte[] bytes) {
        final int startIndex = 0;
        int i;
        for (i = bytes.length - 1; i >= startIndex; i--) {
            bytes[i]++;
            if (bytes[i] != 0)
                break;
        }
        // we return false when all bytes are 0 again
        return (i >= startIndex || bytes[startIndex] != 0);
    }

    /**
     * Utility function to copy a byte array into a new byte array with given size.
     * If the src length is smaller than the given size, the result will be left-padded
     * with zeros.
     *
     * @param value - a BigInteger with a maximum value of 2^256-1
     * @return Byte array of given size with a copy of the <code>src</code>
     */
    public static byte[] copyToArray(BigInteger value) {
        byte[] src = ByteUtil.bigIntegerToBytes(value);
        byte[] dest = ByteBuffer.allocate(32).array();
        System.arraycopy(src, 0, dest, dest.length - src.length, src.length);
        return dest;
    }


    public static ByteArrayWrapper wrap(byte[] data) {
        return new ByteArrayWrapper(data);
    }

    public static byte[] setBit(byte[] data, int pos, int val) {

        if ((data.length * 8) - 1 < pos)
            throw new Error("outside byte array limit, pos: " + pos);

        int posByte = data.length - 1 - (pos) / 8;
        int posBit = (pos) % 8;
        byte setter = (byte) (1 << (posBit));
        byte toBeSet = data[posByte];
        byte result;
        if (val == 1)
            result = (byte) (toBeSet | setter);
        else
            result = (byte) (toBeSet & ~setter);

        data[posByte] = result;
        return data;
    }

    public static int getBit(byte[] data, int pos) {

        if ((data.length * 8) - 1 < pos)
            throw new Error("outside byte array limit, pos: " + pos);

        int posByte = data.length - 1 - pos / 8;
        int posBit = pos % 8;
        byte dataByte = data[posByte];
        return Math.min(1, (dataByte & (1 << (posBit))));
    }

    public static byte[] and(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) throw new RuntimeException("Array sizes differ");
        byte[] ret = new byte[b1.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (byte) (b1[i] & b2[i]);
        }
        return ret;
    }

    public static byte[] or(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) throw new RuntimeException("Array sizes differ");
        byte[] ret = new byte[b1.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (byte) (b1[i] | b2[i]);
        }
        return ret;
    }

    public static byte[] xor(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) throw new RuntimeException("Array sizes differ");
        byte[] ret = new byte[b1.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (byte) (b1[i] ^ b2[i]);
        }
        return ret;
    }

    /**
     * @param arrays - arrays to merge
     * @return - merged array
     */
    public static byte[] merge(byte[]... arrays)
    {
        int arrCount = 0;
        int count = 0;
        for (byte[] array: arrays)
        {
            arrCount++;
            count += array.length;
        }

        // Create new array and copy all array contents
        byte[] mergedArray = new byte[count];
        int start = 0;
        for (byte[] array: arrays) {
            System.arraycopy(array, 0, mergedArray, start, array.length);
            start += array.length;
        }
        return mergedArray;
    }

    public static boolean isNullOrZeroArray(byte[] array){
        return (array == null) || (array.length == 0);
    }

    public static boolean isSingleZero(byte[] array){
        return (array.length == 1 && array[0] == 0);
    }


    public static Set<byte[]> difference(Set<byte[]> setA, Set<byte[]> setB){

        Set<byte[]> result = new HashSet<>();

        for (byte[] elementA : setA){
            boolean found = false;
            for (byte[] elementB : setB){

                if (Arrays.equals(elementA, elementB)){
                    found = true;
                    break;
                }
            }
            if (!found) result.add(elementA);
        }

        return result;
    }

    public static int length(byte[]... bytes) {
        int result = 0;
        for (byte[] array : bytes) {
            result += (array == null) ? 0 : array.length;
        }
        return result;
    }

    public static byte[] intsToBytes(int[] arr, boolean bigEndian) {
        byte[] ret = new byte[arr.length * 4];
        intsToBytes(arr,ret, bigEndian);
        return ret;
    }

    public static int[] bytesToInts(byte[] arr, boolean bigEndian) {
        int[] ret = new int[arr.length / 4];
        bytesToInts(arr, ret, bigEndian);
        return ret;
    }

    public static void bytesToInts(byte[] b, int[] arr, boolean bigEndian) {
        if (!bigEndian) {
            int off = 0;
            for (int i = 0; i < arr.length; i++) {
                int ii = b[off++] & 0x000000FF;
                ii |= (b[off++] << 8) & 0x0000FF00;
                ii |= (b[off++] << 16) & 0x00FF0000;
                ii |= (b[off++] << 24);
                arr[i] = ii;
            }
        } else {
            int off = 0;
            for (int i = 0; i < arr.length; i++) {
                int ii = b[off++] << 24;
                ii |= (b[off++] << 16) & 0x00FF0000;
                ii |= (b[off++] << 8) & 0x0000FF00;
                ii |= b[off++] & 0x000000FF;
                arr[i] = ii;
            }
        }
    }

    public static void intsToBytes(int[] arr, byte[] b, boolean bigEndian) {
        if (!bigEndian) {
            int off = 0;
            for (int i = 0; i < arr.length; i++) {
                int ii = arr[i];
                b[off++] = (byte) (ii & 0xFF);
                b[off++] = (byte) ((ii >> 8) & 0xFF);
                b[off++] = (byte) ((ii >> 16) & 0xFF);
                b[off++] = (byte) ((ii >> 24) & 0xFF);
            }
        } else {
            int off = 0;
            for (int i = 0; i < arr.length; i++) {
                int ii = arr[i];
                b[off++] = (byte) ((ii >> 24) & 0xFF);
                b[off++] = (byte) ((ii >> 16) & 0xFF);
                b[off++] = (byte) ((ii >> 8) & 0xFF);
                b[off++] = (byte) (ii & 0xFF);
            }
        }
    }

    /**
     * Converts 4 bytes IPv4 IP to String representation
     */
    public static String bytesToIp(byte[] bytesIp) {

        StringBuilder sb = new StringBuilder();
        sb.append(bytesIp[0] & 0xFF);
        sb.append(".");
        sb.append(bytesIp[1] & 0xFF);
        sb.append(".");
        sb.append(bytesIp[2] & 0xFF);
        sb.append(".");
        sb.append(bytesIp[3] & 0xFF);

        String ip = sb.toString();
        return ip;
    }

    /**
     * Remove hex string prefix '0x'
     */
    public static String removeHexPrefix(String data) {
        if (data.substring(0, 2).equals("0x"))
            return data.substring(2);
        return data;
    }

    /**
     * Utility method to check if one byte array starts with a specified sequence
     * of bytes.
     *
     * @param array
     *          The array to check
     * @param prefix
     *          The prefix bytes to test for
     * @return true if the array starts with the bytes from the prefix
     */
    public static boolean startsWith(byte[] array, byte[] prefix) {
        if (array == prefix) {
            return true;
        }
        if (array == null || prefix == null) {
            return false;
        }
        int prefixLength = prefix.length;

        if (prefix.length > array.length) {
            return false;
        }

        for (int i = 0; i < prefixLength; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }
}
