package io.taucoin.torrent.publishing.core.utils;

import android.annotation.SuppressLint;
import android.content.Context;

import io.taucoin.torrent.publishing.R;

import static java.lang.String.format;

public class Formatter {

    public static String formatFileSize(Context context, long number){
        return formatFileSize(context, number, false);
    }

    public static String formatShortFileSize(Context context, long number){
        return formatFileSize(context, number, true);
    }

    @SuppressLint("DefaultLocale")
    private static String formatFileSize(Context context, long number, boolean shorter) {
        if (context == null) {
            return "";
        }
        final int unit = 1024;
        float result = number;
        int suffix = R.string.byteShort;
        if (result > 900) {
            suffix = R.string.kilobyteShort;
            result = result / unit;
        }
        if (result > 900) {
            suffix = R.string.megabyteShort;
            result = result / unit;
        }
        if (result > 900) {
            suffix = R.string.gigabyteShort;
            result = result / unit;
        }
        if (result > 900) {
            suffix = R.string.terabyteShort;
            result = result / unit;
        }
        if (result > 900) {
            suffix = R.string.petabyteShort;
            result = result / unit;
        }
        String value;
        if (result < 1) {
            value = format("%.2f", result);
        } else if (result < 10) {
            if (shorter) {
                value = format("%.1f", result);
            } else {
                value = format("%.2f", result);
            }
        } else if (result < 100) {
            if (shorter) {
                value = format("%.0f", result);
            } else {
                value = format("%.2f", result);
            }
        } else {
            value = format("%.0f", result);
        }
        return value + " " + context.getString(suffix);
    }
}
