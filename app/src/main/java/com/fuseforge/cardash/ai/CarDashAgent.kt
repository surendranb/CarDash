package com.fuseforge.cardash.ai

import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SimpleSQLiteQuery
import com.fuseforge.cardash.data.PreferencesManager
import com.fuseforge.cardash.data.db.AppDatabase
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
            tripId TEXT,
            timestamp INTEGER, -- epoch time
            avgRpm INTEGER,
            maxRpm INTEGER,
            avgSpeed INTEGER,
            maxSpeed INTEGER,
            avgEngineLoad INTEGER,
            maxCoolantTemp INTEGER,
            avgThrottlePosition INTEGER,
            avgIntakeAirTemp INTEGER,
            minBatteryVoltage REAL,
            maxBatteryVoltage REAL,
            avgBatteryVoltage REAL,
            fuelLevel INTEGER,
            activeSeconds INTEGER,
            idlingSeconds INTEGER
        )
    """.trimIndent()

    suspend fun executeAgenticLoop(userPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = prefs.getGeminiApiKey()
        if (apiKey.isBlank()) {
            return@withContext "Error: No Gemini API Key found. Please add your token in Settings."
        }

        // Initialize the Gemma/Gemini model using the user's configured identifier
        val modelName = prefs.getGeminiModelName().ifBlank { "gemini-1.5-pro-latest" }
        val model = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey
        )

        try {
            // STEP 1: Generate SQL
            val sqlPrompt = """
                You are an expert SQL engineer. Your task is to write a SQLite query to answer the user's question, based on the following local schema:
                $schemaContext
                
                Important constraints:
                - Output ONLY valid SQLite code. No markdown formatting, no explanations, no `sql` blocks, just the raw SELECT query.
                - Only `SELECT` statements are permitted.
                - Assume `timestamp` is epoch milliseconds. Use proper datetime conversion if filtering by time.
                
                USER QUESTION: "$userPrompt"
            """.trimIndent()

            var sqlQuery = model.generateContent(sqlPrompt).text?.trim() ?: ""
            // Clean markdown if the model hallucinates formatting
            sqlQuery = sqlQuery.removePrefix("```sql").removePrefix("```").removeSuffix("```").trim()

            if (!sqlQuery.uppercase().startsWith("SELECT")) {
                return@withContext "Error: Agent generated invalid or unsafe SQL.\nQuery: $sqlQuery"
            }

            // STEP 2: Execute Data Extraction
            val csvData = runQueryToCSV(sqlQuery)

            // STEP 3: Analyze Results
            val analysisPrompt = """
                You are a master digital mechanic analyzing my vehicle's high-fidelity ledger data.
                I asked the following question: "$userPrompt"
                
                We ran this exact query against my local CarDash database:
                `$sqlQuery`
                
                And received these results:
                $csvData
                
                Please evaluate these results and provide a friendly, insightful, and highly technical response explaining what this data means for the health of my car. Format securely with Markdown.
            """.trimIndent()

            val response = model.generateContent(analysisPrompt).text ?: "Agent failed to analyze the results."
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
