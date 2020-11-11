package io.taucoin.torrent.publishing.core.utils;

import android.view.View;
import android.widget.TextView;

import java.math.BigInteger;

public class StringUtil {

    public static String formatString(String str, Object... replace) {
        try {
            if (isNotEmpty(str)) {
                str = String.format(str, replace);
            }
        } catch (Exception ignore) {
        }

        return str;
    }

    public static String formatString(String str, double replace) {
        try {
            if (isNotEmpty(str)) {
                str = String.format(str, String.valueOf(replace));
            }
        } catch (Exception ignore) {
        }
        return str;
    }

    public static boolean isEquals(CharSequence a, CharSequence b) {
        if (isEmpty(a)) {
            return isEmpty(b);
        } else if (isEmpty(b)) {
            return isEmpty(a);
        } else {
            return a.equals(b);
        }
    }

    public static boolean isNotEquals(CharSequence a, CharSequence b) {
        return !isEquals(a, b);
    }

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }

    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    public static boolean equals(CharSequence a, CharSequence b) {
        if (a == b) {
            return true;
        }
        int length;
        if (a != null && b != null && (length = a.length()) == b.length()) {
            if (a instanceof String && b instanceof String) {
                return a.equals(b);
            } else {
                for (int i = 0; i < length; i++) {
                    if (a.charAt(i) != b.charAt(i)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
    public static String trim(String string) {
        if (isEmpty(string)) {
            return "";
        }
        return string.trim();
    }

    /**
     * <p>Bytes to hex string</p>
     *
     * @param bytes       this source bytes array
     * @param lengthToPad length to pad
     * @return hex string
     */
    public static String toHexString(byte[] bytes, int lengthToPad) {
        BigInteger hash = new BigInteger(1, bytes);
        String digest = hash.toString(16);

        while (digest.length() < lengthToPad) {
            digest = "0" + digest;
        }
        return digest;
    }

    public static double toDouble(String num) {
        try{
            return Double.parseDouble(num);
        }catch (Exception e){
            return 0;
        }
    }

    public static String getText(TextView view) {
        try{
            return view.getText().toString();
        }catch (Exception e){
            return "";
        }
    }

    public static int getIntText(TextView view) {
        try{
            return Integer.parseInt(getText(view));
        }catch (Exception e){
            return 0;
        }
    }

    public static String getTag(View view) {
        try{
            return view.getTag().toString();
        }catch (Exception e){
            return "";
        }
    }

    public static int getIntTag(View view) {
        try{
            return Integer.parseInt(getTag(view));
        }catch (Exception e){
            return 0;
        }
    }

    public static int getIntString(String data) {
        try{
            return Integer.parseInt(data);
        }catch (Exception e){
            return 0;
        }
    }

    public static long getLongString(String data) {
        try{
            return Long.parseLong(data);
        }catch (Exception e){
            return 0;
        }
    }

    public static double getDoubleString(String data) {
        try{
            return Double.parseDouble(data);
        }catch (Exception e){
            return 0;
        }
    }

    public static String getString(int data) {
        try{
            return String.valueOf(data);
        }catch (Exception e){
            return "";
        }
    }

    public static String getString(Object data) {
        try{
            return data.toString();
        }catch (Exception e){
            return "";
        }
    }

    public static String encryptPhone(String photo) {
        if(isNotEmpty(photo) && photo.length() > 7){
            photo = photo.substring(0, 3) + "****" + photo.substring(7);
        }
        return photo;
    }
    public static boolean isAddressValid(String address) {
        String regex = "^T[a-zA-Z0-9_]{33,}$";
        return address.matches(regex);
    }

    public static double getProgress(long height, long maxHeight) {
        double progress = 0;
        if(height > 0){
            progress = (double) height * 100 / maxHeight;
        }
        if(progress < 0){
            progress = 0;
        }
        if(progress > 100){
            progress = 100;
        }
        return progress;
    }

    public static String getPlusOrMinus(String value) {
        if(isNotEmpty(value)){
            if(value.startsWith("+")){
                return "+";
            }else if(value.startsWith("-")){
                return "-";
            }
        }
        return "";
    }

    public static String changeMiningRank(String oldValue, long newValue) {
        long value = StringUtil.getLongString(oldValue);
        String type = StringUtil.getPlusOrMinus(oldValue);
        value = Math.abs(value);
        String changeValue = "";
        if(StringUtil.isEmpty(oldValue) || value == newValue){
            changeValue += type;
        }else{
            if(value < newValue){
                changeValue += "-";
            }else{
                changeValue += "+";
            }
        }
        changeValue += newValue;
        return changeValue;
    }

    /**
     * 获取名字的首字母
     * @param groupName 名字
     * @return firstLetters
     */
    public static String getFirstLettersOfName(String groupName) {
        StringBuilder firstLetters = new StringBuilder();
        String blankChar = "&nbsp;";
        if(isNotEmpty(groupName)){
            String[] splits;
            if(groupName.indexOf(blankChar) > 0){
                splits = groupName.split(blankChar);
            }else {
                splits = groupName.split(" ");
            }
            for (String split : splits) {
                if(firstLetters.length() >= 3){
                    break;
                }
                if(isNotEmpty(split)){
                    String firstLetter = split.substring(0, 1);
                    if(firstLetters.length() == 0){
                        firstLetter = firstLetter.toUpperCase();
                    }else{
                        firstLetter = firstLetter.toLowerCase();
                    }
                    firstLetters.append(firstLetter);
                }
            }
        }
        return firstLetters.toString();
    }

    /**
     * 截取字符串后num个字符
     * @param str 原字符串
     * @param num 个数
     * @return 处理后字符串
     */
    public static String subStringLater(String str, int num){
        if(isNotEmpty(str) && str.length() > num){
            return str.substring(str.length() - num);
        }
        return str;
    }
}
