package com.fuseforge.cardash.utils

import android.content.Context
import android.util.Log
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.data.db.TripDataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID
import kotlin.random.Random

object MockDataSeeder {

    suspend fun seedRecentData(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.obdLogDao()
            val calendar = Calendar.getInstance()
            val tripId = UUID.randomUUID().toString()
            
            Log.d("MockDataSeeder", "Starting to seed mock data...")

            // Generate 100 data points spread over the last 7 days
            for (i in 1..100) {
                // Random time in the last 7 days
                val randomHoursAgo = Random.nextInt(1, 168)
                val pointTime = calendar.clone() as Calendar
                pointTime.add(Calendar.HOUR_OF_DAY, -randomHoursAgo)

                val dataPoint = TripDataPoint(
                    tripId = tripId,
                    timestamp = pointTime.time,
                    rpm = Random.nextInt(1500, 4500),
                    engineLoad = Random.nextInt(20, 85),
                    coolantTemp = Random.nextInt(80, 105), // Normal to slightly high
                    intakeAirTemp = Random.nextInt(20, 45),
                    speedObd = Random.nextInt(0, 120),
                    batteryVoltage = 12.0f + Random.nextFloat() * 2.5f, // 12.0 - 14.5V
                    fuelLevel = Random.nextInt(10, 100)
                )

                dao.insertTripDataPoint(dataPoint)
            }
            
            Log.d("MockDataSeeder", "Successfully seeded 100 mock TripDataPoints.")
            return@withContext true
        } catch (e: Exception) {
            Log.e("MockDataSeeder", "Failed to seed data", e)
            return@withContext false
        }
    }
}
