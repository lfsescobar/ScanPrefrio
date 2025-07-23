package com.ingeneo.scanprefrio.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
            entities = [
            ScanRecord::class,
            ClienteEntity::class,
            TipoFlorEntity::class,
            VariedadEntity::class
        ], 
    version = 4, 
    exportSchema = false
)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    
    companion object {
        @Volatile
        private var INSTANCE: ScanDatabase? = null
        
        fun getDatabase(context: Context): ScanDatabase {
            return INSTANCE ?: synchronized(this) {
                        val instance = Room.databaseBuilder(
            context.applicationContext,
            ScanDatabase::class.java,
            "scan_database"
        )
            .fallbackToDestructiveMigration()
            .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
