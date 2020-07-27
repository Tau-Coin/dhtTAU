package io.taucoin.torrent.publishing.core.storage.sqlite;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;

/**
 * 数据库升级迁移类
 */
class DatabaseMigration {

    static Migration[] getMigrations(@NonNull Context appContext) {
        return new Migration[] {
//                MIGRATION_1_2
        };
    }

//    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database)
//        {
//            database.execSQL("ALTER TABLE torrents ADD COLUMN downloading_metadata integer ");
//            database.execSQL("ALTER TABLE torrents ADD COLUMN datetime integer ");
//        }
//    };
}