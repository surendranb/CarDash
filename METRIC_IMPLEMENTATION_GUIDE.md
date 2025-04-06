# Metric Implementation Guide

## Development Methodology

1. **Single Responsibility Principle**:
   - Each metric has its own dedicated files
   - Clear separation between UI, business logic, and data layer

2. **Test-Driven Development**:
   - Write unit tests for parsing logic first
   - Verify with mock OBD responses before live testing

3. **Incremental Implementation**:
   - Implement one metric at a time
   - Verify each step before proceeding

## Implementation Steps

### 1. Research & Planning
- [ ] Identify OBD2 PID code for the metric
- [ ] Research response format and parsing requirements
- [ ] Document expected value ranges and units

### 2. Service Layer Implementation
- [ ] Add command constant to OBDService companion object
- [ ] Implement sendCommand() wrapper method
- [ ] Create parseResponse() function
- [ ] Add Flow property for live updates

### 3. ViewModel Integration
- [ ] Extend MetricViewModel to handle new metric
- [ ] Add state management for the metric
- [ ] Implement threshold coloring logic

### 4. UI Components
- [ ] Create dedicated MetricCard composable
- [ ] Add to MetricGridScreen layout
- [ ] Implement proper formatting/units display

### 5. Testing & Validation
- [ ] Unit test parsing logic
- [ ] Verify with physical OBD2 scanner
- [ ] Test edge cases (disconnections, invalid responses)

## File Structure

```
app/src/main/java/com/example/cardash/
├── services/obd/
│   └── OBDService.kt          # Add command handling
├── ui/metrics/
│   ├── {MetricName}Card.kt    # UI component
│   ├── MetricViewModel.kt     # Business logic
│   └── MetricGrid.kt          # Layout integration
```

## Code Standards

1. **Naming Conventions**:
   - Commands: `PID_SHORT_NAME` (e.g. `ENGINE_LOAD`)
   - Flows: `metricNameFlow` (e.g. `engineLoadFlow`)
   - Cards: `{MetricName}Card` (e.g. `EngineLoadCard`)

2. **Error Handling**:
   - All OBD commands must handle:
     - Connection errors
     - Invalid responses
     - Timeouts

3. **Performance**:
   - Limit polling to necessary frequency
   - Use coroutines properly
   - Avoid UI thread blocking

## Example Implementation (Speed)

```kotlin
// OBDService.kt
companion object {
    const val SPEED_COMMAND = "010D" // PID for Speed
}

val speedFlow: Flow<Int> = flow {
    while(isRunning) {
        emit(sendCommand(SPEED_COMMAND).let { response ->
            // Parsing logic - speed is single byte in km/h
            val values = response.split(" ")
            values[1].toIntOrNull(16) ?: 0
        })
        delay(1000) // Poll every second
    }
}.catch { e ->
    // Error handling
}
```

## Common Pitfalls & Solutions

1. **UI Component Issues**:
   - Ensure all imports are correct (MetricCard, colors, etc.)
   - MetricCard uses `status` parameter (MetricStatus enum) not color
   - Verify @Composable context is properly set

2. **Build Verification**:
   - Always run `./gradlew assembleDebug` before committing
   - Address all compiler errors before proceeding
   - Warnings about deprecated APIs can be handled later

3. **Testing Approach**:
   - Physical testing can be done after basic validation
   - Focus on getting the build working first
   - Use Git to track changes incrementally
```

## Review Checklist
- [ ] Compiles without warnings
- [ ] Passes all unit tests
- [ ] Works with physical OBD2 device
- [ ] Proper error states handled
- [ ] Performance optimized
