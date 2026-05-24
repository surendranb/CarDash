package com.fuseforge.cardash.utils

import android.content.Context
import android.util.Log
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.data.db.VehicleHeartbeat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GeminiPromptBuilder(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)

    /**
     * Builds a deterministic prompt for the "Analyze last N days" preset action.
     * Filters out engine-off noise, computes per-trip summaries, and hands
     * the LLM a structured payload to narrate — no SQL generation needed.
     */
    suspend fun buildPromptForLastNDays(days: Int): String? = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            val endDate = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startDate = calendar.time

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val dateRangeStr = "${dateFormat.format(startDate)} to ${dateFormat.format(endDate)}"

            // Fetch heartbeats and filter to time range + engine-on only
            val allHeartbeats = db.obdLogDao().getRecentHeartbeatsInstant(500)
            val heartbeats = allHeartbeats
                .filter { it.timestamp.after(startDate) }
                .filter { (it.avgRpm ?: 0) > 200 } // Engine-on rows only

            if (heartbeats.isEmpty()) {
                Log.d("GeminiPromptBuilder", "No active driving heartbeats in the last $days days.")
                return@withContext null
            }

            // 1. Global vitals from the deterministic engine
            val vitals = HealthAnalyticsEngine.computeVitals(heartbeats)

            // 2. Per-trip summary table
            val tripGroups = heartbeats.groupBy { it.tripId }
            val tripSummaries = buildTripSummaryTable(tripGroups)

            // 3. Raw ledger table (with all relevant columns)
            val ledgerTable = buildLedgerTable(heartbeats)

            // 4. Vehicle context
            val prefsManager = com.fuseforge.cardash.data.PreferencesManager(context)
            val vehicleStr = prefsManager.getVehicleProfile()
            val vehicleContextStr = if (vehicleStr.isNotBlank()) "I am driving a $vehicleStr." else "I am providing you with general telematics data."

            return@withContext """
                Act as an expert automotive mechanic and data analyst.
                $vehicleContextStr
                I am providing you with my vehicle's deterministic "Digital Clone Ledger" over the last $days days ($dateRangeStr).
                
                IMPORTANT: Engine-off / parked heartbeats have already been filtered out. Every row below represents genuine engine-on driving or idling. The data is clean and trustworthy.
                
                My local analytics engine computed these health scores:
                - Battery Health Score: ${vitals.batteryHealthScore}% (based on lowest crank voltage observed)
                - Thermal Consistency: ${vitals.thermalConsistencyScore}% (% of time at optimal operating temp 90-110°C)
                - Idling Fatigue: ${vitals.idlingFatiguePercentage}% (time spent stationary with engine on)
                - Total Active Driving: ${vitals.totalActiveMinutes} minutes
                - Voltage Stress Detected: ${vitals.voltageStressDetected}
                
                Here is the per-trip summary:
                
                $tripSummaries
                
                And here is the detailed minute-by-minute ledger:
                
                $ledgerTable
                
                Please:
                1. Provide a nuanced, expert assessment of the vehicle's health based on this data.
                2. Identify each trip and describe the driving pattern (city, highway, idling session, etc.)
                3. Point out specific anomalies (voltage dips, temperature spikes, RPM surges).
                4. Offer personalized maintenance advice based on the driving patterns observed.
            """.trimIndent()

        } catch (e: Exception) {
            Log.e("GeminiPromptBuilder", "Error building prompt: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Builds a deterministic prompt for the "Battery Health" preset action.
     */
    suspend fun buildBatteryHealthPrompt(): String? = withContext(Dispatchers.IO) {
        try {
            val heartbeats = db.obdLogDao().getRecentHeartbeatsInstant(500)
                .filter { (it.avgRpm ?: 0) > 200 }

            if (heartbeats.isEmpty()) return@withContext null

            val minVoltage = heartbeats
                .mapNotNull { it.minBatteryVoltage }
                .filter { it > 5.0f }
                .minOrNull() ?: return@withContext null

            val avgVoltage = heartbeats
                .mapNotNull { it.avgBatteryVoltage }
                .filter { it > 5.0f }
                .average()

            val stressEvents = heartbeats.count { (it.minBatteryVoltage ?: 14f) < 11.5f && (it.minBatteryVoltage ?: 14f) > 5f }
            val totalMinutes = heartbeats.sumOf { it.activeSeconds } / 60

            val prefsManager = com.fuseforge.cardash.data.PreferencesManager(context)
            val vehicleStr = prefsManager.getVehicleProfile()

            return@withContext """
                Act as an expert automotive electrician.
                ${if (vehicleStr.isNotBlank()) "Vehicle: $vehicleStr." else ""}
                
                Here are the deterministic battery statistics from my vehicle's onboard ledger:
                - Lowest voltage recorded: ${String.format(Locale.US, "%.2f", minVoltage)}V
                - Average voltage across all driving sessions: ${String.format(Locale.US, "%.2f", avgVoltage)}V
                - Number of voltage stress events (dips below 11.5V): $stressEvents
                - Total monitored driving time: $totalMinutes minutes
                - Data points analyzed: ${heartbeats.size} one-minute heartbeats
                
                Please assess:
                1. Is the battery in good health? What does the lowest voltage indicate?
                2. Are the stress events concerning? What could cause them?
                3. Any maintenance recommendations?
            """.trimIndent()

        } catch (e: Exception) {
            Log.e("GeminiPromptBuilder", "Error building battery prompt: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Builds a deterministic prompt for the "Engine Strain" preset action.
     */
    suspend fun buildEngineStrainPrompt(): String? = withContext(Dispatchers.IO) {
        try {
            val heartbeats = db.obdLogDao().getRecentHeartbeatsInstant(500)
                .filter { (it.avgRpm ?: 0) > 200 }

            if (heartbeats.isEmpty()) return@withContext null

            val movingHeartbeats = heartbeats.filter { (it.avgSpeed ?: 0) > 0 }
            val avgLoadWhileMoving = if (movingHeartbeats.isNotEmpty()) {
                movingHeartbeats.mapNotNull { it.avgEngineLoad }.average()
            } else 0.0

            val avgRpmWhileMoving = if (movingHeartbeats.isNotEmpty()) {
                movingHeartbeats.mapNotNull { it.avgRpm }.average()
            } else 0.0

            val maxRpmSeen = heartbeats.mapNotNull { it.maxRpm }.maxOrNull() ?: 0
            val avgThrottle = movingHeartbeats.mapNotNull { it.avgThrottlePosition }.average()
            val totalMovingMinutes = movingHeartbeats.sumOf { it.activeSeconds - it.idlingSeconds } / 60
            val totalIdlingMinutes = heartbeats.sumOf { it.idlingSeconds } / 60

            val prefsManager = com.fuseforge.cardash.data.PreferencesManager(context)
            val vehicleStr = prefsManager.getVehicleProfile()

            return@withContext """
                Act as an expert automotive performance analyst.
                ${if (vehicleStr.isNotBlank()) "Vehicle: $vehicleStr." else ""}
                
                Here are the deterministic engine strain statistics from my onboard ledger:
                - Average engine load while moving: ${String.format(Locale.US, "%.1f", avgLoadWhileMoving)}%
                - Average RPM while moving: ${String.format(Locale.US, "%.0f", avgRpmWhileMoving)}
                - Maximum RPM recorded: $maxRpmSeen
                - Average throttle position while moving: ${String.format(Locale.US, "%.1f", avgThrottle)}%
                - Total moving time: $totalMovingMinutes minutes
                - Total idling time: $totalIdlingMinutes minutes
                - Data points analyzed: ${heartbeats.size} heartbeats (${movingHeartbeats.size} while moving)
                
                Please assess:
                1. Is the engine being driven hard or gently?
                2. Is the idling-to-driving ratio healthy?
                3. Any concerns from the peak RPM or load figures?
                4. Driving style recommendations?
            """.trimIndent()

        } catch (e: Exception) {
            Log.e("GeminiPromptBuilder", "Error building engine strain prompt: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Builds a prompt for recommending a fuel multiplier.
     */
    suspend fun buildFuelCalibrationPrompt(): String? = withContext(Dispatchers.IO) {
        try {
            val prefsManager = com.fuseforge.cardash.data.PreferencesManager(context)
            val vehicleStr = prefsManager.getVehicleProfile()

            return@withContext """
                Act as an expert automotive telematics engineer.
                ${if (vehicleStr.isNotBlank()) "My vehicle is a: $vehicleStr." else "I have not specified my vehicle profile."}
                
                I am using an OBD-II scanner that reads fuel level via PID 01 2F.
                Some vehicle manufacturers scale their fuel percentage responses differently. 
                Our app uses a "Fuel Multiplier" setting to correct this reading.
                For example:
                - A multiplier of 1.0 means we take the raw percentage as-is.
                - A multiplier of 0.392 (100/255) is often used if the manufacturer reports raw out of 255.
                
                Please:
                1. Based on my vehicle profile, tell me what the correct Fuel Multiplier is likely to be.
                2. Explain how I can calibrate it myself if the guess is wrong (e.g. fill the tank and see what it reads).
                3. Keep the advice simple and action-oriented.
            """.trimIndent()
        } catch (e: Exception) {
            Log.e("GeminiPromptBuilder", "Error building fuel calibration prompt: ${e.message}", e)
            return@withContext null
        }
    }

    private fun buildTripSummaryTable(tripGroups: Map<String, List<VehicleHeartbeat>>): String {
        val header = "| Trip | Date | Duration (min) | Avg Speed | Max Speed | Avg RPM | Max RPM | Avg Load | Fuel Start→End | Min Voltage |\n|---|---|---|---|---|---|---|---|---|---|"
        val rows = tripGroups.map { (tripId, hbs) ->
            val sorted = hbs.sortedBy { it.timestamp }
            val dateStr = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(sorted.first().timestamp)
            val durationMin = sorted.sumOf { it.activeSeconds } / 60
            val avgSpeed = sorted.mapNotNull { it.avgSpeed }.average().let { String.format(Locale.US, "%.0f", it) }
            val maxSpeed = sorted.mapNotNull { it.maxSpeed }.maxOrNull() ?: 0
            val avgRpm = sorted.mapNotNull { it.avgRpm }.average().let { String.format(Locale.US, "%.0f", it) }
            val maxRpm = sorted.mapNotNull { it.maxRpm }.maxOrNull() ?: 0
            val avgLoad = sorted.mapNotNull { it.avgEngineLoad }.average().let { String.format(Locale.US, "%.0f", it) }
            val fuelStart = sorted.firstNotNullOfOrNull { it.fuelLevel }?.let { "${it}%" } ?: "-"
            val fuelEnd = sorted.lastNotNullOfOrNull { it.fuelLevel }?.let { "${it}%" } ?: "-"
            val minVolt = sorted.mapNotNull { it.minBatteryVoltage }.filter { it > 5f }.minOrNull()?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
            val shortId = tripId.take(8)
            "| $shortId | $dateStr | $durationMin | $avgSpeed km/h | $maxSpeed km/h | $avgRpm | $maxRpm | $avgLoad% | $fuelStart→$fuelEnd | ${minVolt}V |"
        }
        return header + "\n" + rows.joinToString("\n")
    }

    private fun buildLedgerTable(heartbeats: List<VehicleHeartbeat>): String {
        val header = "| Timestamp | Trip | Active(s) | Idle(s) | Avg Speed | Avg RPM | Avg Load% | Throttle% | Min Volts | Max Temp°C | Fuel% |\n|---|---|---|---|---|---|---|---|---|---|---|"
        val rows = heartbeats.sortedBy { it.timestamp }.map { hb ->
            val dateStr = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(hb.timestamp)
            val tripShort = hb.tripId.take(8)
            "| $dateStr | $tripShort | ${hb.activeSeconds} | ${hb.idlingSeconds} | ${hb.avgSpeed ?: "-"} | ${hb.avgRpm ?: "-"} | ${hb.avgEngineLoad ?: "-"} | ${hb.avgThrottlePosition ?: "-"} | ${hb.minBatteryVoltage?.let { String.format(Locale.US, "%.1f", it) } ?: "-"} | ${hb.maxCoolantTemp ?: "-"} | ${hb.fuelLevel ?: "-"} |"
        }
        return header + "\n" + rows.joinToString("\n")
    }

    /**
     * Utility extension — last non-null value in a list.
     */
    private fun <T, R : Any> List<T>.lastNotNullOfOrNull(transform: (T) -> R?): R? {
        for (i in lastIndex downTo 0) {
            val result = transform(this[i])
            if (result != null) return result
        }
        return null
    }
}
