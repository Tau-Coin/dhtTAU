///**
// * Copyright 2018 Taucoin Core Developers.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package io.taucoin.torrent.publishing.core.utils;
//
//import android.content.Context;
//import android.content.pm.PackageStats;
//import android.os.Bundle;
//import android.os.IBinder;
//import android.os.IInterface;
//import android.os.Message;
//
//class ResManager{
//
//     private boolean isRunning = true;
//     private IBinder mBinder = null;
//     private IInterface mInterface = null;
//
//     @Override
//     public void handleMessage(Message msg) {
//         switch (msg.what) {
//             case 2:
//                 Bundle bundle = msg.getData();
//                 SysUtil.MemoryInfo info = bundle.getParcelable("data");
//                 if(info != null){
//                     String memoryInfo = SysUtil.formatFileSizeMb(info.totalMemory);
//
//                     String cpuInfo = String.valueOf(info.cpuUsageRate);
//                     int pointIndex = cpuInfo.indexOf(".");
//                     int length = cpuInfo.length();
//                     if(pointIndex > 0 && length - pointIndex > 3){
//                         cpuInfo = cpuInfo.substring(0, pointIndex + 3);
//                     }
//                     cpuInfo += "%";
//
//                     long dailyTraffic = info.netDataSize;
//                     String netDataInfo = SysUtil.formatFileSizeMb(dailyTraffic);
//
//                     if(mResCallBack != null){
//                         mResCallBack.updateCpuAndMemory(cpuInfo, memoryInfo, netDataInfo);
//                     }
//                 }
//                 break;
//             case 3:
//                 PackageStats newPs = msg.getData().getParcelable("data");
//                 if (newPs != null) {
//                     long dataSize = newPs.dataSize + newPs.cacheSize + newPs.codeSize;
//                     dataSize += newPs.externalCacheSize + newPs.externalCodeSize + newPs.externalDataSize;
//                     dataSize += newPs.externalMediaSize + newPs.externalObbSize;
//                     String dataInfo = SysUtil.formatFileSizeMb(dataSize);
//                     if(mResCallBack != null){
//                         mResCallBack.updateDataSize(dataInfo);
//                     }
//                 }
//                 break;
//             default:
//                 break;
//         }
//     }
//
//    private synchronized void startResThreadDelay() {
//         ThreadPool.getThreadPool().execute(() -> {
//             try {
//                 if(isRunning){
//                     Context context = MyApplication.getInstance();
//                     mSysUtil.getPkgInfo(context.getPackageName(), packageStatsObserver);
//
//                     long traffic = TrafficInfo.getTrafficUsed(context);
//                     traffic = traffic >= 0 ? traffic : 0;
//                     TrafficUtil.saveTrafficAll(traffic);
//
//                     SysUtil.MemoryInfo info =  mSysUtil.loadAppProcess();
//
//                     long summaryTotal = NetworkStatsUtil.getSummaryTotal(context);
//                     if(summaryTotal != -1){
//                         info.netDataSize = summaryTotal;
//                     }else{
//                         info.netDataSize = TrafficUtil.getTrafficTotal();
//                     }
//
//                     Bundle bundle = new Bundle();
//                     bundle.putParcelable("data", info);
//                     Message message = mHandler.obtainMessage(2);
//                     message.setData(bundle);
//                     mHandler.sendMessage(message);
//                     Thread.sleep(3500);
//                     startResThreadDelay();
//                 }
//             } catch (InterruptedException e) {
//                 Logger.e("startResThreadDelay is error", e);
//             }
//         });
//     }
//
//    long traffic = TrafficInfo.getTrafficUsed(context);
//    traffic = traffic >= 0 ? traffic : 0;
//                     TrafficUtil.saveTrafficAll(traffic);
//
//    SysUtil.MemoryInfo info =  mSysUtil.loadAppProcess();
//
//    long summaryTotal = NetworkStatsUtil.getSummaryTotal(context);
//                     if(summaryTotal != -1){
//        info.netDataSize = summaryTotal;
//    }else{
//        info.netDataSize = TrafficUtil.getTrafficTotal();
//    }
//
//
// }