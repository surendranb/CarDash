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

            // 2. Fetch the data
            val dataPointsFlow = db.obdLogDao().getTripDataPointsByTimeRange(startDate, endDate)
            val dataPoints = dataPointsFlow.firstOrNull()

            if (dataPoints.isNullOrEmpty()) {
                return@withContext null
            }

            // 3. Aggregate data
            val summary = aggregateData(dataPoints)

            // 4. Construct the prompt
            return@withContext """
                Act as an expert automotive mechanic and data analyst. 
                I am providing you with OBD2 telematics data collected from my vehicle over the last $days days ($dateRangeStr).
                
                Please analyze this data and provide:
                1. A general health assessment of the vehicle.
                2. Any anomalies or areas of concern (e.g., high temperatures, low battery, erratic RPMs).
                3. Driving habit insights based on speed, RPM, and engine load.
                4. Recommendations for upcoming maintenance based on this snapshot.

                Here is the aggregated data summary:
                
                - Total Data Points Recorded: ${dataPoints.size}
                
                **Engine Performance:**
                - Average RPM: ${summary.avgRpm}
                - Max RPM: ${summary.maxRpm}
                - Average Engine Load: ${summary.avgLoad}%
                - Max Engine Load: ${summary.maxLoad}%
                
                **Temperatures:**
                - Average Coolant Temp: ${summary.avgCoolantTemp}°C
                - Max Coolant Temp: ${summary.maxCoolantTemp}°C
                - Average Intake Air Temp: ${summary.avgIntakeTemp}°C
                - Max Intake Air Temp: ${summary.maxIntakeTemp}°C
                
                **Speeds:**
                - Average Speed (OBD): ${summary.avgSpeed} km/h
                - Max Speed (OBD): ${summary.maxSpeed} km/h
                
                **Electrical / Fuel:**
                - Average Battery Voltage: ${summary.avgVoltage}V
                - Min Battery Voltage: ${summary.minVoltage}V
                - Average Fuel Level: ${summary.avgFuel}%
                
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
