package com.fuseforge.cardash.utils

import android.content.Context
import android.util.Log
import com.fuseforge.cardash.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GeminiPromptBuilder(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)

    suspend fun buildPromptForLastNDays(days: Int): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Calculate time range (for context string)
            val calendar = Calendar.getInstance()
            val endDate = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startDate = calendar.time

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val dateRangeStr = "${dateFormat.format(startDate)} to ${dateFormat.format(endDate)}"

            // 2. Fetch the deterministc Ledger Heartbeats
            val heartbeats = db.obdLogDao().getRecentHeartbeatsInstant(250) // Enough for a few hours of driving
            val recentHeartbeats = heartbeats.filter { it.timestamp.after(startDate) }

            if (recentHeartbeats.isEmpty()) {
                Log.d("GeminiPromptBuilder", "No heartbeats found in the last $days days.")
                return@withContext null
            }

            // 3. Format the Ledger Data for the LLM
            val tableHeader = "| Timestamp | Active (s) | Idle (s) | Avg RPM | Max RPM | Avg Load (%) | Min Volts | Max Temp (°C) |\n|---|---|---|---|---|---|---|---|"
            val rows = mutableListOf<String>()
            
            for (hb in recentHeartbeats) {
                val dateStr = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(hb.timestamp)
                rows.add("| $dateStr | ${hb.activeSeconds} | ${hb.idlingSeconds} | ${hb.avgRpm ?: "-"} | ${hb.maxRpm ?: "-"} | ${hb.avgEngineLoad ?: "-"} | ${hb.minBatteryVoltage?.let{String.format(Locale.US, "%.1f", it)} ?: "-"} | ${hb.maxCoolantTemp ?: "-"} |")
            }

            val tableMarkdown = tableHeader + "\n" + rows.joinToString("\n")

            val prefsManager = com.fuseforge.cardash.data.PreferencesManager(context)
            val vehicleStr = prefsManager.getVehicleProfile()
            val vehicleContextStr = if (vehicleStr.isNotBlank()) "I am driving a $vehicleStr." else "I am providing you with general telematics data."

            // 4. Run the deterministic analytics engine (Task 3.1 logic) as a summary for the LLM
            val vitals = HealthAnalyticsEngine.computeVitals(recentHeartbeats)
            
            // 5. Construct the prompt
            return@withContext """
                Act as an expert automotive mechanic and data analyst.
                $vehicleContextStr
                I am providing you with the deterministic "Digital Clone Ledger" (1 row = 1 minute) of my vehicle's health over the last $days days ($dateRangeStr).
                
                My local, onboard analytics engine has already computed the following trends for this period:
                - Battery Health Score (Crank voltage capability): ${vitals.batteryHealthScore}%
                - Thermal Consistency Score (Reached optimal operating temp): ${vitals.thermalConsistencyScore}%
                - Idling Fatigue (City traffic standing ratio): ${vitals.idlingFatiguePercentage}%
                - Severe Voltage Stress Detected: ${vitals.voltageStressDetected}
                
                Please review the local insights along with the tabular ledger data below to:
                1. Provide a nuanced, expert assessment of the vehicle's health.
                2. Point out specific anomalies in the ledger data (e.g. voltage dropping too hard at idle, or RPM surges).
                3. Offer personalized maintenance advice based on my city/highway driving patterns observed here.

                Here is the tabular history of my recent vitals:
                
                $tableMarkdown
                
            """.trimIndent()

        } catch (e: Exception) {
            Log.e("GeminiPromptBuilder", "Error building prompt: ${e.message}", e)
            return@withContext null
        }
    }
}
