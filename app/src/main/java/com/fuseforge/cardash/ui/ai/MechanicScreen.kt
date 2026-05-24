package com.fuseforge.cardash.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fuseforge.cardash.ai.CarDashAgent
import com.fuseforge.cardash.data.PreferencesManager
import com.fuseforge.cardash.utils.GeminiPromptBuilder
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicScreen() {
    val context = LocalContext.current
    val agent = remember { CarDashAgent(context) }
    val promptBuilder = remember { GeminiPromptBuilder(context) }
    val prefsManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    
    var currentQuery by remember { mutableStateOf("") }
    var chatHistory by remember { 
        mutableStateOf(try {
            val jsonStr = prefsManager.getChatHistory()
            if (jsonStr.isBlank()) emptyList()
            else {
                val array = JSONArray(jsonStr)
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    ChatMessage(obj.getString("text"), obj.getBoolean("isUser"))
                }
            }
        } catch (e: Exception) { emptyList<ChatMessage>() })
    }
    var isThinking by remember { mutableStateOf(false) }

    fun saveHistory(history: List<ChatMessage>) {
        val array = JSONArray()
        for (msg in history) {
            val obj = JSONObject()
            obj.put("text", msg.text)
            obj.put("isUser", msg.isUser)
            array.put(obj)
        }
        prefsManager.setChatHistory(array.toString())
    }

    /**
     * Agentic path: free-form user queries → LLM classifies, writes SQL, narrates.
     */
    fun submitFreeformQuery(query: String) {
        if (query.isBlank() || isThinking) return
        
        val contextList = chatHistory.toList()
        
        chatHistory = chatHistory + ChatMessage(query, isUser = true)
        saveHistory(chatHistory)
        currentQuery = ""
        isThinking = true
        
        scope.launch {
            val response = agent.executeAgenticLoop(query, contextList)
            chatHistory = chatHistory + ChatMessage(response, isUser = false)
            saveHistory(chatHistory)
            isThinking = false
        }
    }

    /**
     * Deterministic path: preset actions → hardcoded data extraction → single LLM narration call.
     * Numbers are always correct. One API call. Fast.
     */
    fun submitPresetAction(label: String, buildPrompt: suspend () -> String?) {
        if (isThinking) return

        chatHistory = chatHistory + ChatMessage(label, isUser = true)
        saveHistory(chatHistory)
        isThinking = true

        scope.launch {
            try {
                val prompt = buildPrompt()
                if (prompt == null) {
                    val noDataMsg = "No driving data found. Take a drive with CarDash connected and come back!"
                    chatHistory = chatHistory + ChatMessage(noDataMsg, isUser = false)
                    saveHistory(chatHistory)
                    isThinking = false
                    return@launch
                }

                val apiKey = prefsManager.getGeminiApiKey()
                if (apiKey.isBlank()) {
                    val errorMsg = "Error: No Gemini API Key found. Please add your token in Settings."
                    chatHistory = chatHistory + ChatMessage(errorMsg, isUser = false)
                    saveHistory(chatHistory)
                    isThinking = false
                    return@launch
                }

                val modelName = prefsManager.getGeminiModelName().ifBlank { "gemini-1.5-pro-latest" }
                val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
                val response = model.generateContent(prompt).text
                    ?: "Failed to generate analysis."

                chatHistory = chatHistory + ChatMessage(response, isUser = false)
                saveHistory(chatHistory)
            } catch (e: Exception) {
                val errorMsg = "Analysis Error: ${e.message}\nEnsure your API token is valid."
                chatHistory = chatHistory + ChatMessage(errorMsg, isUser = false)
                saveHistory(chatHistory)
            } finally {
                isThinking = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Digital Mechanic",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Quick Actions — deterministic path
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedFilterChip(
                selected = false,
                onClick = {
                    submitPresetAction("Analyze the drive for the last 7 days.") {
                        promptBuilder.buildPromptForLastNDays(7)
                    }
                },
                label = { Text("7-Day Analysis") }
            )
            ElevatedFilterChip(
                selected = false,
                onClick = {
                    submitPresetAction("What was my lowest battery voltage recorded?") {
                        promptBuilder.buildBatteryHealthPrompt()
                    }
                },
                label = { Text("Battery Health") }
            )
            ElevatedFilterChip(
                selected = false,
                onClick = {
                    submitPresetAction("What is my average engine load while moving?") {
                        promptBuilder.buildEngineStrainPrompt()
                    }
                },
                label = { Text("Engine Strain") }
            )
            ElevatedFilterChip(
                selected = false,
                onClick = {
                    submitPresetAction("What should my fuel multiplier be?") {
                        promptBuilder.buildFuelCalibrationPrompt()
                    }
                },
                label = { Text("Fuel Multiplier") }
            )
        }

        Divider(modifier = Modifier.padding(bottom = 16.dp))

        // Chat History
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatHistory.size) { index ->
                val message = chatHistory[index]
                MessageBubble(message)
            }
            if (isThinking) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Field — agentic path (free-form)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentQuery,
                onValueChange = { currentQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask your mechanic...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = { submitFreeformQuery(currentQuery) },
                enabled = currentQuery.isNotBlank() && !isThinking
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 600.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)
