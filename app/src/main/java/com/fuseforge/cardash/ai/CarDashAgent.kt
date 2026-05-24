package com.fuseforge.cardash.ai

import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SimpleSQLiteQuery
import com.fuseforge.cardash.data.PreferencesManager
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.ui.ai.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CarDashAgent(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val prefs = PreferencesManager(context)

    // The SQL schema of the digital clone ledger
    private val schemaContext = """
        TABLE vehicle_heartbeats (
            id INTEGER PRIMARY KEY,
            tripId TEXT,                -- session UUID, groups rows into trips
            timestamp INTEGER,          -- epoch time (milliseconds)
            avgRpm INTEGER,             -- average RPM for this 1-minute window
            maxRpm INTEGER,
            avgSpeed INTEGER,           -- average speed in km/h
            maxSpeed INTEGER,
            avgEngineLoad INTEGER,      -- 0-100%
            maxCoolantTemp INTEGER,     -- degrees Celsius
            avgThrottlePosition INTEGER,
            avgIntakeAirTemp INTEGER,
            minBatteryVoltage REAL,     -- lowest voltage in this window (ignore values < 5.0)
            maxBatteryVoltage REAL,
            avgBatteryVoltage REAL,
            fuelLevel INTEGER,          -- percentage (may have a calibration multiplier applied)
            baroPressure INTEGER,
            fuelPressure INTEGER,
            ambientAirTemp INTEGER,
            maf REAL,
            activeSeconds INTEGER,      -- seconds the engine was running in this window
            idlingSeconds INTEGER,      -- of those active seconds, how many were at 0 speed
            incidentCount INTEGER
        )
        
        IMPORTANT: This table contains rows logged while the engine was OFF (OBD adapter stays powered).
        Engine-off rows have avgRpm <= 200 and avgSpeed = 0. You MUST filter them out for any
        meaningful analysis. Always include: WHERE avgRpm > 200
    """.trimIndent()

    suspend fun executeAgenticLoop(userPrompt: String, chatHistory: List<ChatMessage> = emptyList()): String = withContext(Dispatchers.IO) {
        val apiKey = prefs.getGeminiApiKey()
        if (apiKey.isBlank()) {
            return@withContext "Error: No Gemini API Key found. Please add your token in Settings."
        }

        val systemInstructionText = """
            You are "Digital Mechanic", the virtual automotive technician built into CarDash.
            Your role is to help the user diagnose issues, understand telemetry logs, and configure their dashboard.
            
            Vehicle Profile Context:
            - Make & Model: ${prefs.getVehicleProfile().ifBlank { "Unspecified vehicle" }}
            - Current Fuel Gauge Multiplier: ${prefs.getFuelMultiplier()}
            
            Renault/Nissan Fuel Calibration Specs:
            - Renault/Nissan CMF-A+ platform vehicles (Renault Kiger, Renault Triber, Nissan Magnite) physically have a 40L tank capacity (35L usable).
            - The ECU broadcasts the fuel level PID 01 2F scaled against a 70L nominal capacity.
            - This causes the app to read lower than the physical cluster (e.g. 23% read vs 40% actual).
            - To correct this, a scaling multiplier must be applied:
              - 1.75 for 40L physical capacity (70 / 40 = 1.75)
              - 2.00 for 35L capacity (70 / 35 = 2.00)
            
            Calibration Guidance:
            - If the user asks about incorrect fuel gauges, fuel level percentages, or mentions their Kiger/Triber/Magnite fuel mismatch, explain the 70L nominal ECU vs physical capacity discrepancy.
            - Recommend the user to manually enter the correct multiplier (e.g., 1.75 for Kiger, or 2.00 for 35L) under "Fuel Gauge Calibration" in the Settings tab.
            - Explain the math: "Multiplier = Nominal Capacity (70L) / Physical Capacity (your vehicle's tank size)."
            - Do not output any trigger tags or attempt automatic changes; simply provide conversational guidance.
        """.trimIndent()

        val modelName = prefs.getGeminiModelName().ifBlank { "gemini-1.5-pro-latest" }
        val model = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            systemInstruction = content { text(systemInstructionText) }
        )

        try {
            // STEP 1: Classify the user query into TELEMETRY or CHAT
            val routePrompt = """
                You are a query classifier for a vehicle dashboard app (CarDash).
                Determine if the user's question requires querying a SQLite database of historical driving logs (trips, speed, RPM, coolant temperature, fuel levels) to answer it.
                We have a database table `vehicle_heartbeats` containing columns like: avgRpm, maxRpm, avgSpeed, maxSpeed, avgEngineLoad, maxCoolantTemp, fuelLevel, activeSeconds, incidentCount, etc.
                
                Respond with exactly:
                - "TELEMETRY" if the user wants database statistics, metrics, summaries, or log analysis from their trips.
                - "CHAT" if the user is greeting, asking how settings work, requesting advice, asking for calculations (like fuel multipliers), or doing general troubleshooting conversation.
                
                USER QUESTION: "$userPrompt"
                
                Classification (TELEMETRY or CHAT):
            """.trimIndent()

            val classifierModel = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            )
            val routeResponse = classifierModel.generateContent(routePrompt).text?.trim()?.uppercase() ?: "CHAT"
            
            val recentHistoryChunks = chatHistory.takeLast(10).map { msg ->
                content(role = if (msg.isUser) "user" else "model") {
                    text(msg.text)
                }
            }
            val chatSession = model.startChat(history = recentHistoryChunks)

            val response = if (routeResponse.contains("TELEMETRY")) {
                // STEP 2: Generate SQL for telemetry request
                val sqlPrompt = """
                    You are an expert SQL engineer. Your task is to write a SQLite query to answer the user's question, based on the following local schema:
                    $schemaContext
                    
                    MANDATORY constraints:
                    - Output ONLY valid SQLite code. No markdown formatting, no explanations, no ```sql``` blocks, just the raw SELECT query.
                    - Only SELECT statements are permitted.
                    - Assume `timestamp` is epoch milliseconds. Use proper datetime conversion if filtering by time.
                    - EVERY query MUST include `WHERE avgRpm > 200` (or as an AND condition) to exclude engine-off noise rows. This is non-negotiable.
                    
                    Query design guidance:
                    - For average speed, also filter `avgSpeed > 0` to exclude stationary idling.
                    - For driving duration, SUM(activeSeconds) only from rows WHERE avgRpm > 200.
                    - Use `tripId` to group data by individual driving sessions when asked about trips.
                    - For battery voltage, also filter `minBatteryVoltage > 5.0` to exclude hardware glitches.
                    
                    USER QUESTION: "$userPrompt"
                """.trimIndent()

                var sqlQuery = model.generateContent(sqlPrompt).text?.trim() ?: ""
                sqlQuery = sqlQuery.removePrefix("```sql").removePrefix("```").removeSuffix("```").trim()

                if (!sqlQuery.uppercase().startsWith("SELECT")) {
                    "Error: Agent generated invalid or unsafe SQL.\nQuery: $sqlQuery"
                } else {
                    val csvData = runQueryToCSV(sqlQuery)
                    val analysisPayload = """
                        [SYSTEM: Hidden Context Payload]
                        I asked the following question: "$userPrompt"
                        
                        You ran this query on the backend: `$sqlQuery`
                        
                        And retrieved this raw telemetry payload:
                        ```csv
                        $csvData
                        ```
                        
                        Provide a friendly, insightful, and highly technical response explaining what this data means for my car.
                    """.trimIndent()
                    chatSession.sendMessage(analysisPayload).text ?: "Agent failed to analyze the results."
                }
            } else {
                // STEP 2: Conversational CHAT response
                chatSession.sendMessage(userPrompt).text ?: "Agent failed to respond."
            }

            return@withContext response

        } catch (e: Exception) {
            return@withContext "Agent Execution Error: ${e.message}\nEnsure your API token is valid."
        }
    }

    private fun runQueryToCSV(sql: String): String {
        return try {
            val cursor: Cursor = db.query(SimpleSQLiteQuery(sql))
            val sb = java.lang.StringBuilder()
            
            // Header
            val columns = cursor.columnNames
            sb.append(columns.joinToString(", ")).append("\n")

            // Rows
            while (cursor.moveToNext()) {
                val row = mutableListOf<String>()
                for (i in 0 until cursor.columnCount) {
                    val value = when (cursor.getType(i)) {
                        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i).toString()
                        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i).toString()
                        Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                        Cursor.FIELD_TYPE_NULL -> "NULL"
                        else -> "BLOB"
                    }
                    row.add(value)
                }
                sb.append(row.joinToString(", ")).append("\n")
            }
            cursor.close()
            
            if (sb.isEmpty() || sb.toString() == columns.joinToString(", ") + "\n") {
                "No results found."
            } else {
                sb.toString()
            }
        } catch (e: Exception) {
            "Error executing local query: ${e.message}"
        }
    }
}
