package com.fuseforge.cardash.services.cloud

import android.util.Log
import com.fuseforge.cardash.data.db.VehicleHeartbeat
import com.google.auth.oauth2.GoogleCredentials
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class BigQuerySyncer(private val serviceAccountJson: String, private val datasetId: String, private val tableId: String) {
    private val TAG = "BigQuerySyncer"
    private val gson = Gson()
    
    // Parse projectId from JSON
    private val projectId: String? = try {
        val map = gson.fromJson(serviceAccountJson, Map::class.java)
        map["project_id"] as? String
    } catch (e: Exception) {
        null
    }

    private var accessToken: String? = null
    private var tokenExpiry: Long = 0

    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return@withContext accessToken
        }
        try {
            val credentials = GoogleCredentials
                .fromStream(ByteArrayInputStream(serviceAccountJson.toByteArray()))
                .createScoped(listOf("https://www.googleapis.com/auth/bigquery.insertdata"))
            
            credentials.refresh()
            accessToken = credentials.accessToken.tokenValue
            tokenExpiry = credentials.accessToken.expirationTime.time - 60000 // Buffer 1 min
            return@withContext accessToken
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate Google Credentials: ${e.message}")
            null
        }
    }

    suspend fun syncBatch(heartbeats: List<VehicleHeartbeat>): Boolean {
        if (heartbeats.isEmpty()) return true
        
        if (projectId.isNullOrBlank()) {
            Log.e(TAG, "Missing project_id in Service Account JSON")
            return false
        }
        
        val token = getAccessToken()
        if (token == null) {
            Log.e(TAG, "Missing Access Token")
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://bigquery.googleapis.com/bigquery/v2/projects/$projectId/datasets/$datasetId/tables/$tableId/insertAll")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true

                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                
                val rows = heartbeats.map { heartbeat ->
                    val rowMap = mapOf(
                        "tripId" to heartbeat.tripId,
                        "timestamp" to dateFormat.format(heartbeat.timestamp),
                        "activeSeconds" to heartbeat.activeSeconds,
                        "idlingSeconds" to heartbeat.idlingSeconds,
                        "avgRpm" to heartbeat.avgRpm,
                        "avgEngineLoad" to heartbeat.avgEngineLoad,
                        "avgSpeed" to heartbeat.avgSpeed,
                        "avgThrottlePosition" to heartbeat.avgThrottlePosition,
                        "avgIntakeAirTemp" to heartbeat.avgIntakeAirTemp,
                        "avgBatteryVoltage" to heartbeat.avgBatteryVoltage,
                        "minBatteryVoltage" to heartbeat.minBatteryVoltage,
                        "maxBatteryVoltage" to heartbeat.maxBatteryVoltage,
                        "maxSpeed" to heartbeat.maxSpeed,
                        "maxRpm" to heartbeat.maxRpm,
                        "maxCoolantTemp" to heartbeat.maxCoolantTemp,
                        "baroPressure" to heartbeat.baroPressure,
                        "fuelLevel" to heartbeat.fuelLevel
                    )
                    mapOf(
                        "insertId" to UUID.randomUUID().toString(),
                        "json" to rowMap
                    )
                }

                val payloadMap = mapOf("rows" to rows)
                val payloadString = gson.toJson(payloadMap)
                
                conn.outputStream.write(payloadString.toByteArray())
                conn.outputStream.flush()
                conn.outputStream.close()

                val responseCode = conn.responseCode
                var success = false
                if (responseCode in 200..299) {
                    val responseStr = conn.inputStream.bufferedReader().readText()
                    if (responseStr.contains("insertErrors")) {
                        Log.e(TAG, "BigQuery InsertErrors: $responseStr")
                    } else {
                        Log.i(TAG, "Successfully synced ${heartbeats.size} heartbeats to BigQuery")
                        success = true
                    }
                } else {
                    val errorStr = conn.errorStream?.bufferedReader()?.readText()
                    Log.e(TAG, "Failed to sync: HTTP $responseCode - $errorStr")
                }
                conn.disconnect()
                success
            } catch (e: Exception) {
                Log.e(TAG, "Exception syncing to BigQuery: ${e.message}")
                false
            }
        }
    }
}
