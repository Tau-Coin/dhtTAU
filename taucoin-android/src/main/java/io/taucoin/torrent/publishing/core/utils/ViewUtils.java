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

import android.view.View;
import android.widget.TextView;

public class ViewUtils {

    public static String getText(TextView view) {
        return view.getText().toString().trim();
    }

    public static String getStringTag(View view) {
        Object tag = view.getTag();
        if(tag != null){
            return view.getTag().toString().trim();
        }
        return "";
    }

    public static int getIntTag(View view) {
        String tag = getStringTag(view);
        try {
            return Integer.parseInt(tag);
        }catch (Exception ignore){
        }
        return 0;
    }

    public static long getLongTag(View view) {
        String tag = getStringTag(view);
        try {
            return Long.parseLong(tag);
        }catch (Exception ignore){
        }
        return 0L;
    }
}
