package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        EqPresetEntity::class,
        ScanFolderEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SonicFlowDatabase : RoomDatabase() {
    abstract fun sonicFlowDao(): SonicFlowDao

    companion object {
        @Volatile
        private var INSTANCE: SonicFlowDatabase? = null

        fun getInstance(context: Context): SonicFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SonicFlowDatabase::class.java,
                    "sonicflow.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
