package com.fuseforge.cardash.ui.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fuseforge.cardash.ui.components.MetricCard
import com.fuseforge.cardash.ui.components.MetricCardType
import com.fuseforge.cardash.ui.components.MetricStatus
import com.fuseforge.cardash.ui.components.TinyStatusIndicators
import com.fuseforge.cardash.ui.theme.CarDashTheme
import com.fuseforge.cardash.ui.theme.Neutral
import com.fuseforge.cardash.ui.theme.Success
import com.fuseforge.cardash.ui.theme.Warning
import com.fuseforge.cardash.ui.theme.Error as ThemeError

@Composable
fun MetricGridScreen(
    viewModel: MetricViewModel = viewModel(),
    removeEngineStatus: Boolean = false
) {
    val rpm by viewModel.rpm.collectAsState()
    val engineLoad by viewModel.engineLoad.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val coolantTemp by viewModel.coolantTemp.collectAsState()
    val fuelLevel by viewModel.fuelLevel.collectAsState()
    val intakeAirTemp by viewModel.intakeAirTemp.collectAsState()
    val throttlePosition by viewModel.throttlePosition.collectAsState()
    val fuelPressure by viewModel.fuelPressure.collectAsState()
    val baroPressure by viewModel.baroPressure.collectAsState()
    val batteryVoltage by viewModel.batteryVoltage.collectAsState()
    
    // New metric values - simplified
    // We're not using these for now
    // val averageSpeed by viewModel.averageSpeed.collectAsState()
    // val fuelLevelHistory by viewModel.fuelLevelHistory.collectAsState()
    
    // Get engine state
    val engineRunning by viewModel.engineRunning.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isConnected = connectionState is MetricViewModel.ConnectionState.Connected
    
    // Get screen configuration
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    
    // Determine if we're on a tablet (using width as the primary indicator)
    val isTablet = screenWidth >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = if (isTablet) 20.dp else 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Engine status indicator (only shown if not removed)
            if (!removeEngineStatus) {
                EngineStatusIndicator(
                    engineRunning = engineRunning,
                    isConnected = isConnected
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (isTablet) {
                // Tablet layout - optimize for a 10-inch screen
                TabletMetricGrid(
                    rpm = rpm,
                    speed = speed,
                    engineLoad = engineLoad,
                    coolantTemp = coolantTemp,
                    fuelLevel = fuelLevel,
                    throttlePosition = throttlePosition,
                    intakeAirTemp = intakeAirTemp,
                    batteryVoltage = batteryVoltage,
                    fuelPressure = fuelPressure,
                    baroPressure = baroPressure,
                    isConnected = isConnected
                )
            } else {
                // Phone layout - scrollable
                PhoneMetricGrid(
                    rpm = rpm,
                    speed = speed,
                    engineLoad = engineLoad,
                    coolantTemp = coolantTemp,
                    fuelLevel = fuelLevel,
                    throttlePosition = throttlePosition,
                    intakeAirTemp = intakeAirTemp,
                    batteryVoltage = batteryVoltage,
                    fuelPressure = fuelPressure,
                    baroPressure = baroPressure,
                    isConnected = isConnected
                )
            }
        }
    }
}

@Composable
fun TabletMetricGrid(
    rpm: Int,
    speed: Int,
    engineLoad: Int,
    coolantTemp: Int,
    fuelLevel: Int,
    throttlePosition: Int,
    intakeAirTemp: Int,
    batteryVoltage: Float,
    fuelPressure: Int,
    baroPressure: Int,
    isConnected: Boolean
) {
    // Get engine state from the ViewModel
    val viewModel: MetricViewModel = viewModel()
    val engineRunning by viewModel.engineRunning.collectAsState()

    // Use CompactMetricCard for more efficient space usage on tablets
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Tiny status indicators in the top left
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            TinyStatusIndicators(
                engineRunning = engineRunning,
                isConnected = isConnected
            )
        }
        
        // Top row - Primary metrics (3 columns)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // RPM with range
            // Normal: 600-5500 RPM
            // Warning: 0-600 RPM (too low/stalling) or 5500-6500 RPM (high rev)
            // Error: >6500 RPM (redline/dangerous)
            val rpmStatus = getMetricStatus(rpm.toDouble(), 0.0, 600.0, 5500.0, 6500.0)
            val rpmPercentage = (rpm.toFloat() / 7000f).coerceIn(0f, 1f)
            
            MetricCard(
                title = "RPM",
                value = rpm.toString(),
                unit = "rpm",
                status = rpmStatus,
                type = MetricCardType.RANGE,
                currentPercentage = rpmPercentage,
                minValue = 0f,
                maxValue = 7000f,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
            
            // Speed with range
            // Normal: 0-120 km/h
            // Warning: 120-160 km/h (high speed)
            // Error: >160 km/h (excessive/dangerous speed)
            val speedStatus = getMetricStatus(speed.toDouble(), -1.0, -1.0, 120.0, 160.0)
            val speedPercentage = (speed.toFloat() / 180f).coerceIn(0f, 1f)
            
            MetricCard(
                title = "SPEED",
                value = speed.toString(),
                unit = "km/h",
                status = speedStatus,
                type = MetricCardType.RANGE,
                currentPercentage = speedPercentage,
                minValue = 0f,
                maxValue = 180f,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
            
            // Coolant Temperature (Range)
            // Normal: 60-90°C
            // Warning: 0-60°C (too cold) or 90-110°C (getting hot)
            // Error: <0°C (freezing risk) or >110°C (overheating)
            val coolantStatus = getMetricStatus(coolantTemp.toDouble(), 0.0, 60.0, 90.0, 110.0)
            val coolantPercentage = ((coolantTemp.toFloat() - 0f) / 120f).coerceIn(0f, 1f)
            
            MetricCard(
                title = "COOLANT TEMPERATURE",
                value = coolantTemp.toString(),
                unit = "°C",
                status = coolantStatus,
                type = MetricCardType.RANGE,
                currentPercentage = coolantPercentage,
                minValue = 0f,
                maxValue = 120f,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Middle section - (4 columns)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Engine Load (Progress)
            // Normal: 20-80%
            // Warning: 0-20% (very light load) or 80-90% (high load)
            // Error: >90% (excessive load - potential engine strain)
            val engineLoadStatus = getMetricStatus(engineLoad.toDouble(), -1.0, 20.0, 80.0, 90.0)
            val engineLoadPercentage = (engineLoad.toFloat() / 100f).coerceIn(0f, 1f)
            
            MetricCard(
                title = "ENGINE LOAD",
                value = engineLoad.toString(),
                unit = "%",
                status = engineLoadStatus,
                type = MetricCardType.PROGRESS,
                currentPercentage = engineLoadPercentage,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
            
            // Fuel Level (Progress)
            // Normal: 15-100%
            // Warning: 5-15% (low fuel)
            // Error: <5% (critical low fuel)
            val fuelStatus = getMetricStatus(fuelLevel.toDouble(), 5.0, 15.0, 101.0, 102.0)
            val fuelPercentage = (fuelLevel.toFloat() / 100f).coerceIn(0f, 1f)
            
            MetricCard(
                title = "FUEL LEVEL",
                value = fuelLevel.toString(),
                unit = "%",
                status = fuelStatus,
                type = MetricCardType.PROGRESS,
                currentPercentage = fuelPercentage,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
            
            // Throttle Position (Progress)
            // Normal: 0-90%
            // Warning: 90-100% (full throttle)
            // No error state for throttle position
            val throttleStatus = getMetricStatus(throttlePosition.toDouble(), -1.0, -1.0, 90.0, 101.0)
            val throttlePercentage = (throttlePosition.toFloat() / 100f).coerceIn(0f, 1f)
            
            MetricCard(
                title = "THROTTLE POSITION",
                value = throttlePosition.toString(),
                unit = "%",
                status = throttleStatus,
                type = MetricCardType.PROGRESS,
                currentPercentage = throttlePercentage,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
            
            // Intake Air Temperature (Value)
            // Normal: -10°C to 40°C
            // Warning: -30°C to -10°C (very cold) or 40°C to 60°C (very hot)
            // Error: <-30°C or >60°C (extreme temperature)
            val intakeAirStatus = getMetricStatus(intakeAirTemp.toDouble(), -30.0, -10.0, 40.0, 60.0)
            
            MetricCard(
                title = "INTAKE AIR TEMPERATURE",
                value = intakeAirTemp.toString(),
                unit = "°C",
                status = intakeAirStatus,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bottom row - (3 columns)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Battery Voltage (Value)
            // Normal: 11.5-14.5V
            // Warning: 9.0-11.5V (low battery) or 14.5-15.5V (high charging)
            // Error: <9.0V (critically low) or >15.5V (overcharging)
            val batteryStatus = getMetricStatus(batteryVoltage.toDouble(), 9.0, 11.5, 14.5, 15.5)
            
            MetricCard(
                title = "BATTERY VOLTAGE",
                value = String.format("%.1f", batteryVoltage),
                unit = "V",
                status = batteryStatus,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
            
            // Fuel Pressure (Value) - Using dedicated component
            FuelPressureMetricCard(
                pressure = fuelPressure,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
            
            // Barometric Pressure (Value)
            // Normal: 90-105 kPa
            // Warning: 80-90 kPa (low pressure) or 105-110 kPa (high pressure)
            // Error: <80 kPa or >110 kPa (extreme weather conditions)
            val baroPressureStatus = getMetricStatus(baroPressure.toDouble(), 80.0, 90.0, 105.0, 110.0)
            
            MetricCard(
                title = "BAROMETRIC PRESSURE",
                value = baroPressure.toString(),
                unit = "kPa",
                status = baroPressureStatus,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Add space at the bottom for the OS navigation
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun PhoneMetricGrid(
    rpm: Int,
    speed: Int,
    engineLoad: Int,
    coolantTemp: Int,
    fuelLevel: Int,
    throttlePosition: Int,
    intakeAirTemp: Int,
    batteryVoltage: Float,
    fuelPressure: Int,
    baroPressure: Int,
    isConnected: Boolean
) {
    // Get engine state from the ViewModel
    val viewModel: MetricViewModel = viewModel()
    val engineRunning by viewModel.engineRunning.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Tiny status indicators in the top left
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            TinyStatusIndicators(
                engineRunning = engineRunning,
                isConnected = isConnected
            )
        }
        
        // Primary metrics row (RPM and Speed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // RPM with range
            // Normal: 600-5500 RPM
            // Warning: 0-600 RPM (too low/stalling) or 5500-6500 RPM (high rev)
            // Error: >6500 RPM (redline/dangerous)
            val rpmStatus = getMetricStatus(rpm.toDouble(), 0.0, 600.0, 5500.0, 6500.0)
            val rpmPercentage = (rpm.toFloat() / 7000f).coerceIn(0f, 1f)
            
            MetricCard(
                title = "RPM",
                value = rpm.toString(),
                unit = "rpm",
                status = rpmStatus,
                type = MetricCardType.RANGE,
                currentPercentage = rpmPercentage,
                minValue = 0f,
                maxValue = 7000f,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
            
            // Speed with range
            // Normal: 0-120 km/h
            // Warning: 120-160 km/h (high speed)
            // Error: >160 km/h (excessive/dangerous speed)
            val speedStatus = getMetricStatus(speed.toDouble(), -1.0, -1.0, 120.0, 160.0)
            val speedPercentage = (speed.toFloat() / 180f).coerceIn(0f, 1f)
            
            MetricCard(
                title = "SPEED",
                value = speed.toString(),
                unit = "km/h",
                status = speedStatus,
                type = MetricCardType.RANGE,
                currentPercentage = speedPercentage,
                minValue = 0f,
                maxValue = 180f,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Coolant Temperature
        // Normal: 60-90°C
        // Warning: 0-60°C (too cold) or 90-110°C (getting hot)
        // Error: <0°C (freezing risk) or >110°C (overheating)
        val coolantStatus = getMetricStatus(coolantTemp.toDouble(), 0.0, 60.0, 90.0, 110.0)
        val coolantPercentage = ((coolantTemp.toFloat() - 0f) / 120f).coerceIn(0f, 1f)
        
        MetricCard(
            title = "COOLANT TEMPERATURE",
            value = coolantTemp.toString(),
            unit = "°C",
            status = coolantStatus,
            type = MetricCardType.RANGE,
            currentPercentage = coolantPercentage,
            minValue = 0f,
            maxValue = 120f,
            isConnected = isConnected
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Secondary metrics row (Engine Load, Fuel Level)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Engine Load (Progress)
            // Normal: 20-80%
            // Warning: 0-20% (very light load) or 80-90% (high load)
            // Error: >90% (excessive load - potential engine strain)
            val engineLoadStatus = getMetricStatus(engineLoad.toDouble(), -1.0, 20.0, 80.0, 90.0)
            val engineLoadPercentage = (engineLoad.toFloat() / 100f).coerceIn(0f, 1f)
            
            MetricCard(
                title = "ENGINE LOAD",
                value = engineLoad.toString(),
                unit = "%",
                status = engineLoadStatus,
                type = MetricCardType.PROGRESS,
                currentPercentage = engineLoadPercentage,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
            
            // Fuel Level (Progress)
            // Normal: 15-100%
            // Warning: 5-15% (low fuel)
            // Error: <5% (critical low fuel)
            val fuelStatus = getMetricStatus(fuelLevel.toDouble(), 5.0, 15.0, 101.0, 102.0)
            val fuelPercentage = (fuelLevel.toFloat() / 100f).coerceIn(0f, 1f)
            
            MetricCard(
                title = "FUEL LEVEL",
                value = fuelLevel.toString(),
                unit = "%",
                status = fuelStatus,
                type = MetricCardType.PROGRESS,
                currentPercentage = fuelPercentage,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Third row (Throttle, Intake Air)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Throttle Position (Progress)
            // Normal: 0-90%
            // Warning: 90-100% (full throttle)
            // No error state for throttle position
            val throttleStatus = getMetricStatus(throttlePosition.toDouble(), -1.0, -1.0, 90.0, 101.0)
            val throttlePercentage = (throttlePosition.toFloat() / 100f).coerceIn(0f, 1f)
            
            MetricCard(
                title = "THROTTLE POSITION",
                value = throttlePosition.toString(),
                unit = "%",
                status = throttleStatus,
                type = MetricCardType.PROGRESS,
                currentPercentage = throttlePercentage,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
            
            // Intake Air Temperature (Value)
            // Normal: -10°C to 40°C
            // Warning: -30°C to -10°C (very cold) or 40°C to 60°C (very hot)
            // Error: <-30°C or >60°C (extreme temperature)
            val intakeAirStatus = getMetricStatus(intakeAirTemp.toDouble(), -30.0, -10.0, 40.0, 60.0)
            
            MetricCard(
                title = "INTAKE AIR TEMPERATURE",
                value = intakeAirTemp.toString(),
                unit = "°C",
                status = intakeAirStatus,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Fourth row (Battery, Fuel Pressure)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Battery Voltage (Value)
            // Normal: 11.5-14.5V
            // Warning: 9.0-11.5V (low battery) or 14.5-15.5V (high charging)
            // Error: <9.0V (critically low) or >15.5V (overcharging)
            val batteryStatus = getMetricStatus(batteryVoltage.toDouble(), 9.0, 11.5, 14.5, 15.5)
            
            MetricCard(
                title = "BATTERY VOLTAGE",
                value = String.format("%.1f", batteryVoltage),
                unit = "V",
                status = batteryStatus,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
            
            // Fuel Pressure (Value) - Using dedicated component
            FuelPressureMetricCard(
                pressure = fuelPressure,
                isConnected = isConnected,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Barometric Pressure
        val baroPressureStatus = getMetricStatus(baroPressure.toDouble(), 80.0, 90.0, 105.0, 110.0)
        
        MetricCard(
            title = "BAROMETRIC PRESSURE",
            value = baroPressure.toString(),
            unit = "kPa",
            status = baroPressureStatus,
            isConnected = isConnected
        )
        
        // Extra space at bottom for scrolling past last item and for system navigation
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
fun EngineStatusIndicator(
    engineRunning: Boolean,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, statusText) = when {
        !isConnected -> Triple(
            Neutral.copy(alpha = 0.2f), 
            Neutral, 
            "DISCONNECTED"
        )
        engineRunning -> Triple(
            Success.copy(alpha = 0.2f), 
            Success, 
            "ENGINE RUNNING"
        )
        else -> Triple(
            Warning.copy(alpha = 0.2f), 
            Warning, 
            "ENGINE OFF"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = textColor
        )
    }
}

// Helper function to determine metric status based on value ranges
private fun getMetricStatus(value: Double, critical_low: Double, warning_low: Double, warning_high: Double, critical_high: Double): MetricStatus {
    return when {
        value <= critical_low -> MetricStatus.ERROR
        value <= warning_low -> MetricStatus.WARNING
        value >= critical_high -> MetricStatus.ERROR
        value >= warning_high -> MetricStatus.WARNING
        else -> MetricStatus.GOOD
    }
}