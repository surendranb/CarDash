package com.example.cardash.data.db

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
    
    // Insert a session
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: OBDSession): Long
    
    // Insert a combined reading
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCombinedReading(reading: OBDCombinedReading): Long
    
    // Update session
    @Update
    suspend fun updateSession(session: OBDSession)
    
    // Get active session
    @Query("SELECT * FROM obd_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): OBDSession?
    
    // Get all logs for a session
    @Query("SELECT * FROM obd_logs WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getSessionLogs(sessionId: String): Flow<List<OBDLogEntry>>
    
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

    // Get sessions list
    @Query("SELECT * FROM obd_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<OBDSession>>
    
    // Close active session
    @Query("UPDATE obd_sessions SET isActive = 0, endTime = :endTime WHERE isActive = 1")
    suspend fun closeActiveSessions(endTime: Date = Date())

    // Get session by ID
    @Query("SELECT * FROM obd_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): OBDSession?

    // Get last log entries for metrics dashboard
    @Query("SELECT * FROM obd_logs WHERE dataType IN (:dataTypes) GROUP BY dataType ORDER BY timestamp DESC LIMIT :limit")
    fun getLastMetricEntries(dataTypes: List<OBDDataType>, limit: Int = 10): Flow<List<OBDLogEntry>>
    
    // === METHODS FOR HISTORY SCREEN ===
    
    // Get the last N combined readings
    @Query("SELECT * FROM obd_combined_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getLastCombinedReadings(limit: Int = 25): Flow<List<OBDCombinedReading>>
    
    // Get combined readings for a specific session
    @Query("SELECT * FROM obd_combined_readings WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    fun getSessionCombinedReadings(sessionId: String, limit: Int = 25): Flow<List<OBDCombinedReading>>
    
    // Get combined readings from a specific time period
    @Query("SELECT * FROM obd_combined_readings WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getCombinedReadingsByTimeRange(startTime: Date, endTime: Date): Flow<List<OBDCombinedReading>>
    
    // Clean up old combined readings
    @Query("DELETE FROM obd_combined_readings WHERE timestamp < :cutoffDate")
    suspend fun deleteOldCombinedReadings(cutoffDate: Date)
}