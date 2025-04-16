package com.example.cardash.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        OBDLogEntry::class, 
        OBDSession::class,
        OBDCombinedReading::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun obdLogDao(): OBDLogDao
    
    companion object {
        // Singleton to prevent multiple instances of the database
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            // If INSTANCE is not null, return it
            // If it is null, create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "car_dash_database"
                )
                .fallbackToDestructiveMigration() // Recreate database if migration not defined
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}