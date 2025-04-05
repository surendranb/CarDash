package com.example.cardash.ui.graphs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cardash.ui.theme.CarDashTheme

@Composable
fun GraphScreen(modifier: Modifier = Modifier) {
    Text(
        text = "Graphs Coming Soon",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GraphScreenPreview() {
    CarDashTheme {
        GraphScreen()
    }
}
