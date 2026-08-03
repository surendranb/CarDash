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
        val syncer = BigQuerySyncer(bqJson, datasetId, tableId)
        
        var totalSynced = 0
        var keepSyncing = true

        while (keepSyncing) {
            val unsyncedHeartbeats = obdLogDao.getUnsyncedHeartbeats(limit = 1000)
            
            if (unsyncedHeartbeats.isEmpty()) {
                keepSyncing = false
                continue
            }

            val success = syncer.syncBatch(unsyncedHeartbeats)
            if (success) {
                val ids = unsyncedHeartbeats.map { it.id }
                obdLogDao.markAsSynced(ids)
                totalSynced += ids.size
                prefsManager.setLastSyncTime(System.currentTimeMillis())
            } else {
                Log.e(TAG, "Failed to sync a batch. Stopping early. Will retry later.")
                return@withContext Result.retry()
            }
        }

        if (totalSynced > 0) {
            Log.i(TAG, "Successfully synced and marked $totalSynced heartbeats as synced.")
        } else {
            Log.i(TAG, "No unsynced heartbeats found.")
        }
        
        Result.success()
    }
}
