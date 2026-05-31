package com.fuseforge.cardash.services.cloud

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BigQuerySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "BigQuerySyncWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefsManager = PreferencesManager(applicationContext)
        val bqJson = prefsManager.getBqServiceAccountJson()
        
        if (bqJson.isBlank()) {
            Log.i(TAG, "No BigQuery Service Account configured. Skipping sync.")
            return@withContext Result.success()
        }

        val datasetId = prefsManager.getBqDatasetId()
        val tableId = prefsManager.getBqTableId()

        val database = AppDatabase.getDatabase(applicationContext)
        val obdLogDao = database.obdLogDao()

        val unsyncedHeartbeats = obdLogDao.getUnsyncedHeartbeats(limit = 500)
        
        if (unsyncedHeartbeats.isEmpty()) {
            Log.i(TAG, "No unsynced heartbeats found.")
            return@withContext Result.success()
        }

        Log.i(TAG, "Attempting to sync ${unsyncedHeartbeats.size} heartbeats to BigQuery...")

        val syncer = BigQuerySyncer(bqJson, datasetId, tableId)
        val success = syncer.syncBatch(unsyncedHeartbeats)

        if (success) {
            val ids = unsyncedHeartbeats.map { it.id }
            obdLogDao.markAsSynced(ids)
            prefsManager.setLastSyncTime(System.currentTimeMillis())
            Log.i(TAG, "Successfully synced and marked ${ids.size} heartbeats as synced.")
            Result.success()
        } else {
            Log.e(TAG, "Failed to sync batch. Will retry later.")
            Result.retry()
        }
    }
}
