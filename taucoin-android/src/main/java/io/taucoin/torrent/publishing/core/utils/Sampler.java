package io.taucoin.torrent.publishing.core.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageStats;
import android.os.Build;
import android.os.Debug;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.util.Date;

import androidx.annotation.NonNull;
import io.reactivex.FlowableEmitter;
import io.taucoin.torrent.publishing.MainApplication;

public class Sampler {
    private static final Logger logger = LoggerFactory.getLogger("Sampler");
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
        long cpuTime = 0;
        long appTime = 0;
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
                if (StringUtil.isNotEmpty(procStatString)) {
                    String[] procStats = procStatString.split(" ");
                    if (procStats.length > 8) {
                        cpuTime = Long.parseLong(procStats[2]) + Long.parseLong(procStats[3])
                                + Long.parseLong(procStats[4]) + Long.parseLong(procStats[5])
                                + Long.parseLong(procStats[6]) + Long.parseLong(procStats[7])
                                + Long.parseLong(procStats[8]);
                    }
                }
            }
            if (cpuTime == 0) {
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
            Debug.MemoryInfo dbm;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dbm = new Debug.MemoryInfo();
                Debug.getMemoryInfo(dbm);
            } else {
                Debug.MemoryInfo[] memInfo = activityManager.getProcessMemoryInfo(
                        new int[]{Process.myPid()});
                dbm = memInfo[0];
            }

            // TotalPss = dalvikPss + nativePss + otherPss, in KB
            // getTotalPrivateDirty()就是获得自己进程所独占的内存 in KB
            final int totalPss = dbm.getTotalPss();
            Context context = MainApplication.getInstance();
            logger.trace("sampleMemory maxMemory::{}MB, dalvikPss::{}, nativePss::{}, " +
                    "otherPss::{}, totalPss::{}",
                    activityManager.getMemoryClass(),
                    Formatter.formatFileSize(context, dbm.dalvikPss * 1024),
                    Formatter.formatFileSize(context, dbm.nativePss * 1024),
                    Formatter.formatFileSize(context, dbm.otherPss * 1024),
                    Formatter.formatFileSize(context, dbm.getTotalPss() * 1024));

            if (totalPss >= 0) {
                // Mem in Byte
                mem = totalPss * 1024;
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
        public double cpuUsage;

        public Statistics() {

        }
        Statistics(Parcel in) {
            totalMemory = in.readLong();
            cpuUsageRate = in.readString();
            cpuUsage = in.readDouble();
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
            dest.writeDouble(cpuUsage);
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
