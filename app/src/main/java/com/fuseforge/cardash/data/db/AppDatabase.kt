package com.fuseforge.cardash.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        OBDLogEntry::class, 
        Trip::class, 
        TripDataPoint::class, 
        DiagnosticCode::class,
        VehicleHeartbeat::class
    ],
    version = AppDatabase.VERSION,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun obdLogDao(): OBDLogDao
    abstract fun diagnosticDao(): DiagnosticDao
    
    companion object {
        const val VERSION = 6
        
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
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration() // Recreate database if migration not defined
                .build()
                
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE vehicle_heartbeats ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}