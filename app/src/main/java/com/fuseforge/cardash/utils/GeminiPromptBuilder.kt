package com.fuseforge.cardash.utils

import android.content.Context
import android.util.Log
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.data.db.TripDataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GeminiPromptBuilder(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)

    suspend fun buildPromptForLastNDays(days: Int): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Calculate the time range
            val calendar = Calendar.getInstance()
            val endDate = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startDate = calendar.time

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val dateRangeStr = "${dateFormat.format(startDate)} to ${dateFormat.format(endDate)}"

            // 2. Fetch all trips and filter by date
            val allTripsFlow = db.obdLogDao().getAllTrips()
            val allTrips = allTripsFlow.firstOrNull() ?: emptyList()
            val recentTrips = allTrips.filter { it.startTime.after(startDate) }

            if (recentTrips.isEmpty()) {
                return@withContext null
            }

            // 3. Aggregate data per trip
            val tripRows = mutableListOf<String>()
            val tableHeader = "| Date | Duration | Avg Speed (km/h) | Max Speed | Avg RPM | Max RPM | Avg Load | Max Temp |\n|---|---|---|---|---|---|---|---|"
            
            for (trip in recentTrips) {
                // Fetch up to 10,000 points per trip to ensure we get a good average
                val points = db.obdLogDao().getTripDataPoints(trip.tripId, 10000).firstOrNull() ?: emptyList()
                if (points.isNotEmpty()) {
                    val summary = aggregateData(points)
                    val durationMs = (trip.endTime?.time ?: endDate.time) - trip.startTime.time
                    val durationMins = durationMs / 60000
                    val dateStr = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(trip.startTime)
                    
                    tripRows.add("| $dateStr | ${durationMins}m | ${summary.avgSpeed} | ${summary.maxSpeed} | ${summary.avgRpm} | ${summary.maxRpm} | ${summary.avgLoad}% | ${summary.maxCoolantTemp}°C |")
                }
            }

            if (tripRows.isEmpty()) {
                return@withContext null
            }

            val tableMarkdown = tableHeader + "\n" + tripRows.joinToString("\n")

            val prefsManager = com.fuseforge.cardash.data.PreferencesManager(context)
            val vehicleStr = prefsManager.getVehicleProfile()
            val vehicleContextStr = if (vehicleStr.isNotBlank()) "I am driving a $vehicleStr." else "I am providing you with general telematics data."

            // 4. Construct the prompt
            return@withContext """
                Act as an expert automotive mechanic and data analyst.
                $vehicleContextStr
                I am providing you with a log of my individual driving trips over the last $days days ($dateRangeStr).
                
                Please analyze this tabular trip data and provide:
                1. A general health assessment of the vehicle across these trips.
                2. Any anomalies, trends, or areas of concern (e.g., consistently high temperatures, erratic RPMs on certain trips).
                3. Driving habit insights based on the variations in speed, RPM, duration, and engine load across different trips.
                4. Recommendations for upcoming maintenance based on this snapshot.

                Here is the tabular history of my recent trips:
                
                $tableMarkdown
                
            """.trimIndent()

        } catch (e: Exception) {
            Log.e("GeminiPromptBuilder", "Error building prompt: ${e.message}", e)
            return@withContext null
        }
    }

    private fun aggregateData(points: List<TripDataPoint>): AggregatedStats {
        val validRpms = points.mapNotNull { it.rpm }
        val validLoads = points.mapNotNull { it.engineLoad }
        val validCoolant = points.mapNotNull { it.coolantTemp }
        val validIntake = points.mapNotNull { it.intakeAirTemp }
        val validSpeeds = points.mapNotNull { it.speedObd }
        val validVoltages = points.mapNotNull { it.batteryVoltage }
        val validFuelLevels = points.mapNotNull { it.fuelLevel }

        return AggregatedStats(
            avgRpm = if (validRpms.isNotEmpty()) validRpms.average().toInt() else 0,
            maxRpm = validRpms.maxOrNull() ?: 0,
            avgLoad = if (validLoads.isNotEmpty()) validLoads.average().toInt() else 0,
            maxLoad = validLoads.maxOrNull() ?: 0,
            avgCoolantTemp = if (validCoolant.isNotEmpty()) validCoolant.average().toInt() else 0,
            maxCoolantTemp = validCoolant.maxOrNull() ?: 0,
            avgIntakeTemp = if (validIntake.isNotEmpty()) validIntake.average().toInt() else 0,
            maxIntakeTemp = validIntake.maxOrNull() ?: 0,
            avgSpeed = if (validSpeeds.isNotEmpty()) validSpeeds.average().toInt() else 0,
            maxSpeed = validSpeeds.maxOrNull() ?: 0,
            avgVoltage = if (validVoltages.isNotEmpty()) String.format(Locale.US, "%.1f", validVoltages.average()).toFloat() else 0f,
            minVoltage = validVoltages.minOrNull() ?: 0f,
            avgFuel = if (validFuelLevels.isNotEmpty()) validFuelLevels.average().toInt() else 0
        )
    }

    private data class AggregatedStats(
        val avgRpm: Int, val maxRpm: Int,
        val avgLoad: Int, val maxLoad: Int,
        val avgCoolantTemp: Int, val maxCoolantTemp: Int,
        val avgIntakeTemp: Int, val maxIntakeTemp: Int,
        val avgSpeed: Int, val maxSpeed: Int,
        val avgVoltage: Float, val minVoltage: Float,
        val avgFuel: Int
    )
}
