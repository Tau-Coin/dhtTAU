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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.TrafficStats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TrafficInfo {
    /**
     * Get traffic
     */
    public static NetworkStatistics getTrafficUsed(Context context) {
        if (context == null) {
            return null;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (packageInfo == null) {
                return null;
            }
            int uid = packageInfo.applicationInfo.uid;
            NetworkStatistics statistics = getTraffic(uid);

            if (null == statistics) {
                statistics = getTrafficApi25(uid);
            }
            return statistics;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**traffic
     * Specify uid to get
     */
    private static NetworkStatistics getTraffic(int uid) {
        long txBytes = -1;
        long rxBytes = -1;
        try {
            rxBytes = TrafficStats.getUidRxBytes(uid);
            txBytes = TrafficStats.getUidTxBytes(uid);
            if (rxBytes == -1 && txBytes == -1) {
                return getTrafficUsedByFile(uid);
            }
        } catch (Exception ignore) {
        }
        if(rxBytes == -1 && txBytes == -1){
            return null;
        }else{
            if(rxBytes == -1){
                rxBytes = 0;
            }else if(txBytes == -1){
                txBytes = 0;
            }
        }
        return new NetworkStatistics(txBytes, rxBytes);
    }

    /**
     * Failed to read traffic data for miui8, read system file directly
     */
    private static NetworkStatistics getTrafficUsedByFile(int uid) {
        long receive = -1;
        File receiveFile = new File(String.format("/proc/uid_stat/%s/tcp_rcv", uid));
        BufferedReader buffer = null;
        if (receiveFile.exists()) {
            try {
                buffer = new BufferedReader(new FileReader(receiveFile));
                receive = Long.parseLong(buffer.readLine());
            } catch (Exception ignore) {
            } finally {
                try {
                    if (buffer != null) {
                        buffer.close();
                    }
                } catch (IOException ignore) {
                }
            }
        }
        long send = -1;
        File sendFile = new File(String.format("/proc/uid_stat/%s/tcp_snd", String.valueOf(uid)));
        if (sendFile.exists()) {
            try {
                buffer = new BufferedReader(new FileReader(sendFile));
                send = Long.parseLong(buffer.readLine());
            } catch (Exception ignore) {
            } finally {
                try {
                    if (buffer != null) {
                        buffer.close();
                    }
                } catch (IOException ignore) {
                }
            }
        }

        if (receive == -1 && send == -1) {
            return null;
        }

        receive = (receive == -1) ? 0 : receive;
        send = (send == -1) ? 0 : send;
        return new NetworkStatistics(send, receive);
    }

    /**
     * Higher version traffic acquisition
     * @param uid uid
     * @return long
     */
    private static NetworkStatistics getTrafficApi25(int uid) {
        long receive = 0;
        long send = 0;
        try {
            Method methodGetStatsService = TrafficStats.class.getDeclaredMethod("getStatsService");
            Class classINetworkStatsService = Class.forName("android.net.INetworkStatsService");
            methodGetStatsService.setAccessible(true);
            Object iNetworkStatsService = methodGetStatsService.invoke(null);
            Object netWorkStats = null;
            for (Method method : classINetworkStatsService.getDeclaredMethods()) {
                if ("getDataLayerSnapshotForUid".equals(method.getName())) {
                    method.setAccessible(true);
                    netWorkStats = method.invoke(iNetworkStatsService, uid);
                    break;
                }
            }
            Class classNetWorkStats = Class.forName("android.net.NetworkStats");
            Field[] fields = classNetWorkStats.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
                if ("rxBytes".equals(f.getName())) {
                    long[] rxBytes = (long[]) f.get(netWorkStats);
                    for (int i = 0; i < rxBytes.length; i++) {
                        receive += rxBytes[i];
                    }

                } else if ("txBytes".equals(f.getName())) {
                    long[] txBytes = (long[]) f.get(netWorkStats);
                    for (int i = 0; i < txBytes.length; i++) {
                        send += txBytes[i];
                    }
                }
            }
        } catch (Exception e) {
            receive = -1;
            send = -1;
        }
        if (receive == -1 && send == -1) {
            return null;
        }
        receive = (receive == -1) ? 0 : receive;
        send = (send == -1) ? 0 : send;
        return new NetworkStatistics(send, receive);
    }
}