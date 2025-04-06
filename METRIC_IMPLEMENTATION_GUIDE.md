# CarDash Metric Implementation Guide

## Step 1: Define OBD2 Command
1. Add PID constant to ObdConnectionService companion object:
```kotlin
companion object {
    const val COOLANT_TEMP_COMMAND = "0105"
}
```

## Step 2: Implement OBD Service Layer
1. Add parsing function:
```kotlin
private fun parseCoolantTemp(response: String): Int {
    val values = response.split(" ")
    return if (values.size >= 2) {
        (values[1].toIntOrNull(16) ?: 0) - 40 // Convert to °C
    } else 0
}
```

2. Add data fetching function:
```kotlin 
suspend fun getCoolantTemp(): Int {
    val response = sendCommand(COOLANT_TEMP_COMMAND)
    return parseCoolantTemp(response)
}
```

3. Add Flow for continuous updates:
```kotlin
val coolantTempFlow: Flow<Int> = flow {
    while (isRunning) {
        try {
            emit(getCoolantTemp())
            delay(2000) // Poll every 2 seconds
        } catch (e: Exception) {
            // Handle errors
        }
    }
}.catch { e -> 
    // Handle flow errors
}
```

## Step 3: Update ViewModel
1. Add StateFlow for the metric:
```kotlin
private val _coolantTemp = MutableStateFlow(0)
val coolantTemp = _coolantTemp.asStateFlow()
```

2. Add collection in startDataCollection():
```kotlin
viewModelScope.launch {
    obdConnectionService.coolantTempFlow.collect { temp ->
        _coolantTemp.value = temp
    }
}
```

## Step 4: Create Metric Card UI
1. Create new Composable (e.g. CoolantTempMetricCard.kt):
```kotlin
@Composable
fun CoolantTempMetricCard(
    temp: Int,
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "Coolant Temp",
        value = "$temp°C",
        modifier = modifier
    ) {
        // Custom indicator/visualization
    }
}
```

## Step 5: Add to MetricGrid
1. Add card to MetricGridScreen:
```kotlin
CoolantTempMetricCard(
    temp = viewModel.coolantTemp.value,
    modifier = Modifier.weight(1f)
)
```

## Verification Steps
1. Build and test after each step
2. Verify:
   - Data flows correctly from OBD → ViewModel → UI
   - Error handling works
   - Performance is acceptable

## Best Practices
1. Keep files under 200 lines
2. Single responsibility per component
3. Clear separation between layers:
   - OBD Service (data)
   - ViewModel (logic)
   - UI (presentation)
4. Document all new PIDs and commands
5. Add unit tests for parsing functions
