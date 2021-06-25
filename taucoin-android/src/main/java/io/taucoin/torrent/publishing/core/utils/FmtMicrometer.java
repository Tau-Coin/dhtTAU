/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.torrent.publishing.core.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import io.taucoin.torrent.publishing.core.Constants;

public class FmtMicrometer {
    private static String mDecimal = Constants.COIN.toString(10);
    private static int mScale = mDecimal.length() -1;

    public static String fmtBalance(long balance) {
        DecimalFormat df = getDecimalFormatInstance();
        df.applyPattern("###,##0.########");
        df.setRoundingMode(RoundingMode.FLOOR);
        BigDecimal bigDecimal = new BigDecimal(balance);
        bigDecimal = bigDecimal.divide(new BigDecimal(mDecimal), mScale, RoundingMode.HALF_UP);
        return df.format(bigDecimal);
    }

    static String fmtMiningIncome(long balance) {
        DecimalFormat df = getDecimalFormatInstance();
        df.applyPattern("###,##0.########");
        df.setRoundingMode(RoundingMode.FLOOR);
        BigDecimal bigDecimal = new BigDecimal(balance);
        bigDecimal = bigDecimal.divide(new BigDecimal(mDecimal), mScale, RoundingMode.HALF_UP);
        return df.format(bigDecimal);
    }

    public static String fmtLong(long power) {
        return fmtString(String.valueOf(power));
    }

    public static String fmtString(String power) {
        try {
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("###,##0");
            df.setRoundingMode(RoundingMode.FLOOR);
            BigDecimal bigDecimal = new BigDecimal(power);
            return df.format(bigDecimal);
        }catch (Exception ignore) {

        }
        return new BigInteger("0").toString();
    }

    static String fmtValue(double value) {
        DecimalFormat df = getDecimalFormatInstance();
        df.applyPattern("###,##0.########");
        df.setRoundingMode(RoundingMode.FLOOR);
        BigDecimal bigDecimal = new BigDecimal(value);
        return df.format(bigDecimal);
    }

    public static String fmtDecimal(String value) {
        try {
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("###,##0.########");
            df.setRoundingMode(RoundingMode.FLOOR);
            BigDecimal bigDecimal = new BigDecimal(value);
            return df.format(bigDecimal);
        } catch (Exception ignore) {
        }
        return new BigInteger("0").toString();
    }

    public static String fmtDecimal(double value) {
        return fmtDecimal(String.valueOf(value));
    }

    static DecimalFormat getDecimalFormatInstance() {
        DecimalFormat df;
        try{
            df = (DecimalFormat)NumberFormat.getInstance(Locale.CHINA);
        }catch (Exception e){
            df = new DecimalFormat();
        }
        return df;
    }

    public static String fmtAmount(String amount) {
        try {
            BigDecimal bigDecimal = new BigDecimal(amount);
            bigDecimal = bigDecimal.divide(new BigDecimal(mDecimal), mScale, RoundingMode.HALF_UP);
            return bigDecimal.toString();
        } catch (Exception e) {
            return amount;
        }
    }

    public static long fmtAmount(long amount) {
        try {
            BigDecimal bigDecimal = new BigDecimal(amount);
            bigDecimal = bigDecimal.divide(new BigDecimal(mDecimal), mScale, RoundingMode.HALF_UP);
            return bigDecimal.longValue();
        } catch (Exception e) {
            return 0;
        }
    }

    public static String fmtFormat(String num) {
        try {
            BigDecimal number = new BigDecimal(num);
            number = number.divide(new BigDecimal(mDecimal), mScale, RoundingMode.HALF_UP);
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0.########");
            return df.format(number);
        } catch (Exception e) {
            return num;
        }
    }

    public static String fmtFeeValue(long value) {
        return fmtFeeValue(String.valueOf(value));
    }

    public static String fmtFeeValue(String value) {
        try{
            BigDecimal bigDecimal = new BigDecimal(value);
            bigDecimal = bigDecimal.divide(new BigDecimal(mDecimal), mScale, RoundingMode.HALF_UP);

            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0.##########");
            return df.format(bigDecimal);
        }catch (Exception ignore){

        }
        return new BigInteger("0").toString();
    }

    public static String fmtTxValue(String value) {
        try{
            BigDecimal bigDecimal = new BigDecimal(value);
            bigDecimal = bigDecimal.multiply(new BigDecimal(mDecimal));

            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0");
            return df.format(bigDecimal);
        }catch (Exception ignore){

        }
        return new BigInteger("0").toString();
    }

    public static long fmtTxLongValue(String value) {
        String txValue = fmtTxValue(value);
        return new BigInteger(txValue).longValue();
    }

    public static String fmtFormatFee(String num, String multiply) {
        try {
            BigDecimal number = new BigDecimal(num);
            number = number.multiply(new BigDecimal(multiply));

            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0.########");
            return df.format(number);
        } catch (Exception e) {
            return num;
        }
    }

    public static String fmtFormatAdd(String amount, String fee) {
        try {
            BigDecimal bigDecimal = new BigDecimal(amount);
            bigDecimal = bigDecimal.add(new BigDecimal(fee));
            return bigDecimal.toString();
        } catch (Exception e) {
            return amount;
        }
    }

    public static String formatTwoDecimal(double num) {
        try {
            BigDecimal number = new BigDecimal(num);
            DecimalFormat df = getDecimalFormatInstance();
            df.applyPattern("0.##");
            return df.format(number);
        } catch (Exception e) {
            return new BigInteger("0").toString();
        }
    }

    public static String fmtTestData(long balance) {
        DecimalFormat df = getDecimalFormatInstance();
        df.applyPattern("00000");
        df.setRoundingMode(RoundingMode.FLOOR);
        BigDecimal bigDecimal = new BigDecimal(balance);
        return df.format(bigDecimal);
    }
}
