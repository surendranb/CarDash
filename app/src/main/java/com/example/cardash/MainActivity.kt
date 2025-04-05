package com.example.cardash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cardash.ui.theme.CarDashTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarDashTheme {
                TabScreen()
            }
        }
    }
}

@Composable
fun TabScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Metrics", "Graphs")
    
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            when (selectedTab) {
                0 -> MetricGridScreen()
                1 -> GraphScreen()
            }
        }
    }
}

@Composable
fun MetricGridScreen() {
    // Placeholder for metrics grid
    Text("Metrics Grid Coming Soon")
}

@Composable 
fun GraphScreen() {
    // Placeholder for graphs
    Text("Graphs Coming Soon")
}

@Preview(showBackground = true)
@Composable
fun TabScreenPreview() {
    CarDashTheme {
        TabScreen()
    }
}
