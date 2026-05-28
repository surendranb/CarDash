package com.fuseforge.cardash.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface OBDLogDao {
    
    // Insert a log entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogEntry(logEntry: OBDLogEntry): Long
    
    // Insert a trip
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long
    
    // Insert a trip data point
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripDataPoint(dataPoint: TripDataPoint): Long
    
    // Update trip
    @Update
    suspend fun updateTrip(trip: Trip)
    
    // Get active trip (where endTime is null)
    @Query("SELECT * FROM trips WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveTrip(): Trip?
    
    // Get all logs for a trip
    @Query("SELECT * FROM obd_logs WHERE sessionId = :tripId ORDER BY timestamp DESC")
    fun getSessionLogs(tripId: String): Flow<List<OBDLogEntry>>
    
    // Get all logs regardless of session
    @Query("SELECT * FROM obd_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<OBDLogEntry>>
    
    // Get logs for a specific type
    @Query("SELECT * FROM obd_logs WHERE dataType = :dataType ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsByType(dataType: OBDDataType, limit: Int = 100): Flow<List<OBDLogEntry>>
    
    // Get error logs
    @Query("SELECT * FROM obd_logs WHERE isError = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getErrorLogs(limit: Int = 100): Flow<List<OBDLogEntry>>
    
    // Get logs from a specific time period
    @Query("SELECT * FROM obd_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getLogsByTimeRange(startTime: Date, endTime: Date): Flow<List<OBDLogEntry>>
    
    // Clean up old logs
    @Query("DELETE FROM obd_logs WHERE timestamp < :cutoffDate")
    suspend fun deleteOldLogs(cutoffDate: Date)

    // Get trips list
    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<Trip>>
    
    // Get trips list instantly for one-shot AI prompt generation
    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    suspend fun getAllTripsInstant(): List<Trip>

    // Close active trip
    @Query("UPDATE trips SET endTime = :endTime WHERE endTime IS NULL")
    suspend fun closeActiveTrips(endTime: Date = Date())

    // Get trip by ID
    @Query("SELECT * FROM trips WHERE tripId = :tripId")
    suspend fun getTripById(tripId: String): Trip?

    // Get last log entries for metrics dashboard
    @Query("SELECT * FROM obd_logs WHERE dataType IN (:dataTypes) GROUP BY dataType ORDER BY timestamp DESC LIMIT :limit")
    fun getLastMetricEntries(dataTypes: List<OBDDataType>, limit: Int = 10): Flow<List<OBDLogEntry>>
    
    // === METHODS FOR HISTORY SCREEN ===
    
    // Get the last N combined readings
    @Query("SELECT * FROM trip_data_points ORDER BY timestamp DESC LIMIT :limit")
    fun getLastTripDataPoints(limit: Int = 25): Flow<List<TripDataPoint>>
    
    // Get combined readings for a specific session
    @Query("SELECT * FROM trip_data_points WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT :limit")
    fun getTripDataPoints(tripId: String, limit: Int = 25): Flow<List<TripDataPoint>>
    
    // Get combined readings instantly for a specific session (AI generation)
    @Query("SELECT * FROM trip_data_points WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getTripDataPointsInstant(tripId: String, limit: Int = 10000): List<TripDataPoint>
    
    // Get combined readings from a specific time period
    @Query("SELECT * FROM trip_data_points WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTripDataPointsByTimeRange(startTime: Date, endTime: Date): Flow<List<TripDataPoint>>
    
    // Clean up old combined readings
    @Query("DELETE FROM trip_data_points WHERE timestamp < :cutoffDate")
    suspend fun deleteOldTripDataPoints(cutoffDate: Date)
    
    // === NEW METRICS METHODS ===
    
    // Get fuel levels for the depletion graph (last N readings)
    @Query("SELECT fuelLevel FROM trip_data_points ORDER BY timestamp DESC LIMIT :limit")
    fun getFuelLevelHistory(limit: Int = 12): Flow<List<Int>>
    
    // Get average speed from recent readings (non-zero values only)
    @Query("SELECT AVG(speedObd) FROM trip_data_points WHERE speedObd > 0 AND timestamp > :sinceTime")
    fun getAverageSpeedSince(sinceTime: Date): Flow<Int?>
    
    // Get fuel level readings since 
    @Query("SELECT fuelLevel, timestamp FROM trip_data_points WHERE timestamp > :sinceTime ORDER BY timestamp ASC")
    fun getFuelLevelReadingsSince(sinceTime: Date): Flow<List<FuelLevelReading>>

    // === HEARTBEAT METHODS ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartbeat(heartbeat: VehicleHeartbeat): Long

    @Query("SELECT * FROM vehicle_heartbeats WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getHeartbeatsForTrip(tripId: String): Flow<List<VehicleHeartbeat>>

    @Query("SELECT * FROM vehicle_heartbeats WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getHeartbeatsByTimeRange(startTime: Date, endTime: Date): Flow<List<VehicleHeartbeat>>

    @Query("SELECT * FROM vehicle_heartbeats ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHeartbeats(limit: Int = 100): Flow<List<VehicleHeartbeat>>

    @Query("SELECT * FROM vehicle_heartbeats ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHeartbeatsInstant(limit: Int = 100): List<VehicleHeartbeat>

    @Query("SELECT SUM(avgSpeed * activeSeconds / 3600.0) FROM vehicle_heartbeats")
    fun getTotalDistanceFlow(): Flow<Double?>
    
    @Query("SELECT * FROM vehicle_heartbeats ORDER BY timestamp ASC")
    fun getAllHeartbeatsFlow(): Flow<List<VehicleHeartbeat>>
}