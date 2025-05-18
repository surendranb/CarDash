package com.fuseforge.cardash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.fuseforge.cardash.ui.theme.Success
import com.fuseforge.cardash.ui.theme.Warning
import com.fuseforge.cardash.ui.theme.Neutral
import com.fuseforge.cardash.ui.theme.Error as ThemeError

@Composable
fun TinyStatusIndicators(
    engineRunning: Boolean,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Engine status indicator
        val (engineBgColor, engineText) = when {
            !isConnected -> Pair(Neutral, "OFF")
            engineRunning -> Pair(Success, "ON")
            else -> Pair(Warning, "OFF")
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(engineBgColor.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(engineBgColor)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Status text
            Text(
                text = "Engine $engineText",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = engineBgColor
            )
        }
        
        // OBD connection status indicator
        val (connectionBgColor, connectionText) = when {
            isConnected -> Pair(Success, "ON")
            else -> Pair(Neutral, "OFF")
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(connectionBgColor.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(connectionBgColor)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Status text
            Text(
                text = "OBD $connectionText",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = connectionBgColor
            )
        }
    }
}