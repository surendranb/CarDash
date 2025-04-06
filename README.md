# CarDash

## Project Overview

CarDash is an Android app for OBD2 vehicle diagnostics with real-time metrics and trip history.

## Features

*   Real-time metrics display
*   Trip recording and history
*   Bluetooth OBD2 integration
*   Customizable UI

## Development Progress

### Phase 1: UI Foundation

*   \[x] Initialize Git repository
*   \[x] Create basic tab navigation
*   \[x] Implement metric card components
*   \[x] Set up color threshold system
*   \[x] Create graph view placeholder

### Phase 2: OBD2 Integration (Bluetooth) - COMPLETE

*   \[x] Implement RPM command service via Bluetooth
*   \[x] Handle OBD command parsing/errors (Basic RPM parsing implemented)
*   \[x] Display real-time RPM in metrics grid
*   \[x] Test with physical OBD2 scanner
*   \[x] Add Bluetooth permission handling
*   \[x] Implement device selection UI
*   \[x] Added all 10 planned metrics

### Phase 3: Data Persistence

*   \[ ] Setup Room database
*   \[ ] Implement trip recording
*   \[ ] Store RPM with timestamps
*   \[ ] Create trip history list

### Phase 4: Visualization

*   \[ ] Add line charts for trips
*   \[ ] Implement time filtering
*   \[ ] Enhance card coloring logic

## Metric Implementation Guide

### Development Methodology

1.  **Single Responsibility Principle**:
    *   Each metric has its own dedicated files
    *   Clear separation between UI, business logic, and data layer
2.  **Test-Driven Development**:
    *   Write unit tests for parsing logic first
    *   Verify with mock OBD responses before live testing
3.  **Incremental Implementation**:
    *   Implement one metric at a time
    *   Verify each step before proceeding

### Implementation Steps

1.  Research & Planning
    *   \[ ] Identify OBD2 PID code for the metric
    *   \[ ] Research response format and parsing requirements
    *   \[ ] Document expected value ranges and units
2.  Service Layer Implementation
    *   \[ ] Add command constant to OBDService companion object
    *   \[ ] Implement sendCommand() wrapper method
    *   \[ ] Create parseResponse() function
    *   \[ ] Add Flow property for live updates
3.  ViewModel Integration
    *   \[ ] Extend MetricViewModel to handle new metric
    *   \[ ] Add state management for the metric
    *   \[ ] Implement threshold coloring logic
4.  UI Components
    *   \[ ] Create dedicated MetricCard composable
    *   \[ ] Add to MetricGridScreen layout
    *   \[ ] Implement proper formatting/units display
5.  Testing & Validation
    *   \[ ] Unit test parsing logic
    *   \[ ] Verify with physical OBD2 scanner
    *   \[ ] Test edge cases (disconnections, invalid responses)

### File Structure

    app/src/main/java/com/example/cardash/
    ├── services/obd/
    │   └── OBDService.kt          # Add command handling
    ├── ui/metrics/
    │   ├── {MetricName}Card.kt    # UI component
    │   ├── MetricViewModel.kt     # Business logic
    │   └── MetricGrid.kt          # Layout integration

### Code Standards

1.  **Naming Conventions**:
    *   Commands: `PID_SHORT_NAME` (e.g. `ENGINE_LOAD`)
    *   Flows: `metricNameFlow` (e.g. `engineLoadFlow`)
    *   Cards: `{MetricName}Card` (e.g. `EngineLoadCard`)
2.  **Error Handling**:
    *   All OBD commands must handle:
        *   Connection errors
        *   Invalid responses
        *   Timeouts
3.  **Performance**:
    *   Limit polling to necessary frequency
    *   Use coroutines properly
    *   Avoid UI thread blocking

### Example Implementation (Speed)

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

### Common Pitfalls & Solutions

1.  **UI Component Issues**:
    *   Ensure all imports are correct (MetricCard, MetricStatus, etc.)
    *   MetricCard only accepts these status values from enum:
        *   NORMAL (default safe operating range)
        *   WARNING (approaching limits)
        *   ERROR (dangerous/damaging levels)
    *   Do not create custom status values - use the existing enum
    *   Verify @Composable context is properly set
    *   Include imports from both components and theme packages
2.  **Build Verification**:
    *   Always run `./gradlew assembleDebug` before committing
    *   Address all compiler errors before proceeding
    *   Warnings about deprecated APIs can be handled later
3.  **Testing Approach**:
    *   Physical testing can be done after basic validation
    *   Focus on getting the build working first
    *   Use Git to track changes incrementally

### Review Checklist

*   \[ ] Compiles without warnings
*   \[ ] Passes all unit tests
*   \[ ] Works with physical OBD2 device
*   \[ ] Proper error states handled
*   \[ ] Performance optimized

## Final Metrics Implementation Plan

1.  Throttle Position (PID 0111) - Shows percentage throttle opening
2.  Fuel Pressure (PID 010A) - Important fuel system indicator
3.  Barometric Pressure (PID 0133) - Key atmospheric reading
4.  Timing Advance (PID 010E) - Shows ignition timing adjustment
5.  Battery Voltage (PID 0142) - Control module voltage monitoring

## Future Enhancements

1.  Cloud sync
2.  AI insights
3.  Additional OBD2 parameters
