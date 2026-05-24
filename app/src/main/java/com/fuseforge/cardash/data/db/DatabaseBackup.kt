package com.fuseforge.cardash.data.db

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility to back up the SQLite database before major migrations or resets.
 */
object DatabaseBackup {
    private const val TAG = "DatabaseBackup"
    private const val DB_NAME = "car_dash_database"

    fun backupDatabase(context: Context) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                Log.d(TAG, "No database file found to backup.")
                return
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
            val backupDir = File(baseDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val backupFile = File(backupDir, "${DB_NAME}_v${AppDatabase.VERSION}_$timestamp.bak")
            
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.i(TAG, "Database backup successful: ${backupFile.absolutePath}")
            
            // Also backup -shm and -wal files if they exist (Room/SQLite journal files)
            backupExtra(context, "$DB_NAME-shm", backupDir, timestamp)
            backupExtra(context, "$DB_NAME-wal", backupDir, timestamp)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up database: ${e.message}", e)
        }
    }

    private fun backupExtra(context: Context, fileName: String, backupDir: File, timestamp: String) {
        val extraFile = context.getDatabasePath(fileName)
        if (extraFile.exists()) {
            val backupFile = File(backupDir, "${fileName}_v${AppDatabase.VERSION}_$timestamp.bak")
            FileInputStream(extraFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}
