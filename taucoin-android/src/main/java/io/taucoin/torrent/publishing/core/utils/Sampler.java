package io.taucoin.torrent.publishing.core.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageStats;
import android.os.Debug;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;

import java.io.RandomAccessFile;
import java.util.Date;

import androidx.annotation.NonNull;
import io.reactivex.FlowableEmitter;
import io.taucoin.torrent.publishing.MainApplication;

public class Sampler {
    private volatile static Sampler instance = null;
    private ActivityManager activityManager;
    private Long lastCpuTime;
    private Long lastAppCpuTime;
    private RandomAccessFile procStatFile;
    private RandomAccessFile appStatFile;

    public static Sampler getInstance() {
        if (instance == null) {
            synchronized (Sampler.class) {
                if (instance == null) {
                    instance = new Sampler();
                }
            }
        }
        return instance;
    }

    private Sampler () {
        Context context = MainApplication.getInstance();
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public double sampleCPU() {
        long cpuTime;
        long appTime;
        double sampleValue = 0.0D;
        try {
            if (appStatFile == null) {
                try {
                    procStatFile = new RandomAccessFile("/proc/stat", "r");
                } catch (Exception e) {
                }
                appStatFile = new RandomAccessFile("/proc/" + Process.myPid() + "/stat", "r");
            } else {
                appStatFile.seek(0L);
            }
            if (procStatFile != null) {
                String procStatString = procStatFile.readLine();
                String[] procStats = procStatString.split(" ");
                cpuTime = Long.parseLong(procStats[2]) + Long.parseLong(procStats[3])
                        + Long.parseLong(procStats[4]) + Long.parseLong(procStats[5])
                        + Long.parseLong(procStats[6]) + Long.parseLong(procStats[7])
                        + Long.parseLong(procStats[8]);
            } else {
                Date date = new Date();
                cpuTime = date.getTime();
            }

            String appStatString = appStatFile.readLine();
            String[] appStats = appStatString.split(" ");
            appTime = Long.parseLong(appStats[13]) + Long.parseLong(appStats[14]);
            if (lastCpuTime == null || lastAppCpuTime == null) {
                lastCpuTime = cpuTime;
                lastAppCpuTime = appTime;
                return sampleValue;
            }
            sampleValue = ((double) (appTime - lastAppCpuTime) /
                    (double) (cpuTime - lastCpuTime)) * 100D;
            lastCpuTime = cpuTime;
            lastAppCpuTime = appTime;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sampleValue;
    }

    public long sampleMemory() {
        long mem = 0;
        try {
            // 统计进程的内存信息 totalPss
            final Debug.MemoryInfo[] memInfo = activityManager.getProcessMemoryInfo(
                    new int[]{Process.myPid()});
            if (memInfo.length > 0) {
                // TotalPss = dalvikPss + nativePss + otherPss, in KB
                final int totalPss = memInfo[0].getTotalPss();
                if (totalPss >= 0) {
                    // Mem in Byte
                    mem = totalPss * 1024;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mem;
    }

    public static class Statistics implements Parcelable {
        public long totalMemory;
        public long storageSize;
        public String cpuUsageRate;

        public Statistics() {

        }
        Statistics(Parcel in) {
            totalMemory = in.readLong();
            cpuUsageRate = in.readString();
        }

        public final Creator<Statistics> CREATOR = new Creator<Statistics>() {
            @Override
            public Statistics createFromParcel(Parcel in) {
                return new Statistics(in);
            }

            @Override
            public Statistics[] newArray(int size) {
                return new Statistics[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(totalMemory);
            dest.writeString(cpuUsageRate);
        }
    }


    private static class PackageStatsObserver extends IPackageStatsObserver.Stub {
        FlowableEmitter<Statistics> emitter;

        public void setEmitter(FlowableEmitter<Statistics> emitter) {
            this.emitter = emitter;
        }

        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) {

        }
    }

    public PackageStatsObserver packageStatsObserver = new PackageStatsObserver() {
        private IBinder mBinder;
        private IInterface mInterface;

        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) {
            if (pStats != null) {
                long dataSize = pStats.dataSize + pStats.cacheSize + pStats.codeSize;
                dataSize += pStats.externalCacheSize + pStats.externalCodeSize + pStats.externalDataSize;
                dataSize += pStats.externalMediaSize + pStats.externalObbSize;
                if (emitter != null && !emitter.isCancelled()) {
                    Statistics statistics = new Statistics();
                    statistics.storageSize = dataSize;
                    emitter.onNext(statistics);
                }
            }
        }

        @Override
        public IBinder asBinder() {
            mBinder = super.asBinder();
            return mBinder;
        }

        @Override
        public IInterface queryLocalInterface(@NonNull String descriptor) {
            mInterface = super.queryLocalInterface(descriptor);
            return mInterface;
        }

        public void stopObserver() {
            if(mInterface != null){
                mBinder = mInterface.asBinder();
            }
            mBinder = null;
            mInterface = null;
        }
    };
}
