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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import io.taucoin.torrent.publishing.MainApplication;

/**
 * 复制剪切版管理
 */
public class CopyManager {

    /**
     * 复制文本到剪切版
     */
    public static void copyText(CharSequence copyText) {
        if(StringUtil.isEmpty(copyText)){
            copyText = "";
        }
        Context context = MainApplication.getInstance();
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(null, copyText));
    }

    /**
     * 获取剪切板上的内容
     */
    public static String getClipboardContent(Context context) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            ClipData data = cm.getPrimaryClip();
            if (data != null && data.getItemCount() > 0) {
                ClipData.Item item = data.getItemAt(0);
                if (item != null) {
                    CharSequence sequence = item.coerceToText(context);
                    if (sequence != null) {
                        return sequence.toString();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 清除剪切板上的内容
     */
    public static void clearClipboardContent() {
        copyText( "");
    }
}
