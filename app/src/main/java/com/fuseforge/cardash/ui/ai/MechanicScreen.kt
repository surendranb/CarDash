package com.fuseforge.cardash.ui.ai

import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicScreen() {
    val context = LocalContext.current
    val agent = remember { CarDashAgent(context) }
    val scope = rememberCoroutineScope()
    
    var currentQuery by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isThinking by remember { mutableStateOf(false) }

    fun submitQuery(query: String) {
        if (query.isBlank() || isThinking) return
        
        chatHistory = chatHistory + ChatMessage(query, isUser = true)
        currentQuery = ""
        isThinking = true
        
        scope.launch {
            val response = agent.executeAgenticLoop(query)
            chatHistory = chatHistory + ChatMessage(response, isUser = false)
            isThinking = false
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
        
        // Quick Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedFilterChip(
                selected = false,
                onClick = { submitQuery("Analyze the drive for the last 7 days.") },
                label = { Text("7-Day Analysis") }
            )
            ElevatedFilterChip(
                selected = false,
                onClick = { submitQuery("What was my lowest battery voltage recorded?") },
                label = { Text("Battery Health") }
            )
            ElevatedFilterChip(
                selected = false,
                onClick = { submitQuery("What is my average engine load while moving?") },
                label = { Text("Engine Strain") }
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
                                Text("Analyzing Database...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Field
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
                onClick = { submitQuery(currentQuery) },
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
            modifier = Modifier.widthIn(max = 280.dp)
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
