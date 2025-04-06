# Metric Troubleshooting Log

This document tracks the investigation and resolution of issues related to OBD2 metric accuracy in the CarDash app.

## Metrics Under Review

1.  **RPM** (PID `010C`)
2.  **Speed** (PID `010D`)
3.  **Engine Load** (PID `0104`)
4.  **Coolant Temp** (PID `0105`)
5.  **Fuel Level** (PID `012F`)
6.  **Intake Air Temp** (PID `010F`)
7.  **Throttle Position** (PID `0111`)
8.  **Fuel Pressure** (PID `010A`)
9.  **Barometric Pressure** (PID `0133`)
10. **Battery Voltage** (PID `0142`)

---

## 1. RPM (PID `010C`)

*   **Reported Issue:** Not accurate compared to car's tachometer.
*   **PID:** `01 0C` (Verified Correct)
*   **Standard Formula:** `((A * 256) + B) / 4`, where A and B are the first two data bytes.
*   **Current Implementation (`OBDService.kt::parseRPMResponse`):**
    ```kotlin
    private fun parseRPMResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) { // Potential issue: Should check for >= 3 for 2 data bytes
            val byteA = values[1].toIntOrNull(16) ?: 0 // Potential issue: Indexing might be off if response includes echoed command
            val byteB = if (values.size > 2) values[2].toIntOrNull(16) ?: 0 else 0 // Potential issue: Indexing might be off
            return ((byteA * 256) + byteB) / 4 // Formula correct
        }
        throw Exception("Invalid RPM response")
    }
    ```
*   **Investigation:**
    *   [Date TBD] Formula `((A * 256) + B) / 4` is correct.
    *   [Date TBD] Parsing logic: Assumes response format is `41 0C A B ...`. If the actual response is different (e.g., includes noise, doesn't echo command, different spacing), `values[1]` and `values[2]` might not be the correct data bytes (A and B). Need to verify typical response format. DriveWise example uses `rawData.replace("[\r\n> ]".toRegex(), "")` and then `substring(4, 8)` which is more robust against spacing issues and assumes the first 4 chars are `410C`.
*   **Resolution:**
    *   [Date TBD]
*   **Status:** Pending Investigation

---

## 2. Speed (PID `010D`)

*   **Reported Issue:** Shows non-zero speed when stationary.
*   **PID:** `01 0D` (Verified Correct)
*   **Standard Formula:** `A`, where A is the first data byte = Speed in km/h.
*   **Current Implementation (`OBDService.kt::parseSpeedResponse`):**
    ```kotlin
    private fun parseSpeedResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) { // Potential issue: Should check for >= 3 if response includes echoed command
            return values[1].toIntOrNull(16) ?: 0 // Potential issue: Indexing might be off (should be values[2] if command is echoed?)
        }
        throw Exception("Invalid speed response")
    }
    ```
*   **Investigation:**
    *   [Date TBD] Formula `A` is correct.
    *   [Date TBD] Parsing logic: Assumes response format `41 0D A ...`. If the actual response is different, `values[1]` might not be the correct data byte (A). DriveWise example uses `substring(4, 6)` after cleaning, suggesting the data byte starts at index 4.
*   **Resolution:**
    *   [Date TBD]
*   **Status:** Pending Investigation

---

## 3. Engine Load (PID `0104`)

*   **Reported Issue:** None reported yet, review for correctness.
*   **PID:** `01 04` (Verified Correct)
*   **Standard Formula:** `A * 100 / 255`, where A is the first data byte = Load %.
*   **Current Implementation (`OBDService.kt::parseEngineLoadResponse`):**
    ```kotlin
    private fun parseEngineLoadResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) { // Potential issue: Should check for >= 3
            return values[1].toIntOrNull(16) ?: 0 // Potential issue: Indexing. Formula is incorrect (should be * 100 / 255)
        }
        throw Exception("Invalid engine load response")
    }
    ```
*   **Investigation:**
    *   [Date TBD] Formula is incorrect in the code. It should be `(A * 100) / 255`.
    *   [Date TBD] Parsing logic indexing needs verification based on actual response format. DriveWise example uses `substring(4, 6)`.
*   **Resolution:**
    *   [Date TBD] Correct formula. Adjust parsing index if necessary.
*   **Status:** Pending Investigation

---

## 4. Coolant Temp (PID `0105`)

*   **Reported Issue:** None reported yet, review for correctness.
*   **PID:** `01 05` (Verified Correct)
*   **Standard Formula:** `A - 40`, where A is the first data byte = Temp °C.
*   **Current Implementation (`OBDService.kt::parseCoolantTempResponse`):**
    ```kotlin
    private fun parseCoolantTempResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) { // Potential issue: Should check for >= 3
            return (values[1].toIntOrNull(16) ?: 0) - 40 // Potential issue: Indexing. Formula correct.
        }
        throw Exception("Invalid coolant temp response")
    }
    ```
*   **Investigation:**
    *   [Date TBD] Formula `A - 40` is correct.
    *   [Date TBD] Parsing logic indexing needs verification. DriveWise example uses `substring(4, 6)`.
*   **Resolution:**
    *   [Date TBD]
*   **Status:** Pending Investigation

---

## 5. Fuel Level (PID `012F`)

*   **Reported Issue:** Abnormally low reading.
*   **PID:** `01 2F` (Verified Correct)
*   **Standard Formula:** `A * 100 / 255`, where A is the first data byte = Fuel Level %.
*   **Current Implementation (`OBDService.kt::parseFuelLevelResponse`):**
    ```kotlin
    private fun parseFuelLevelResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) { // Potential issue: Should check for >= 3
            return (values[1].toIntOrNull(16) ?: 0) * 100 / 255 // Potential issue: Indexing. Formula correct.
        }
        throw Exception("Invalid fuel level response")
    }
    ```
*   **Investigation:**
    *   [Date TBD] Formula `A * 100 / 255` is correct.
    *   [Date TBD] Parsing logic indexing needs verification. DriveWise example uses `substring(4, 6)`. The low reading might be due to incorrect indexing grabbing the wrong byte.
*   **Resolution:**
    *   [Date TBD]
*   **Status:** Pending Investigation

---

## 6. Intake Air Temp (PID `010F`)

*   **Reported Issue:** Shows -40 when ignition off (explained), review for correctness when running.
*   **PID:** `01 0F` (Verified Correct)
*   **Standard Formula:** `A - 40`, where A is the first data byte = Temp °C.
*   **Current Implementation (`OBDService.kt::parseIntakeAirTempResponse`):**
    ```kotlin
    private fun parseIntakeAirTempResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) { // Potential issue: Should check for >= 3
            return (values[1].toIntOrNull(16) ?: 0) - 40 // Potential issue: Indexing. Formula correct.
        }
        throw Exception("Invalid intake air temp response")
    }
    ```
*   **Investigation:**
    *   [Date TBD] Formula `A - 40` is correct.
    *   [Date TBD] Parsing logic indexing needs verification.
*   **Resolution:**
    *   [Date TBD]
*   **Status:** Pending Investigation

---

## 7. Throttle Position (PID `0111`)

*   **Reported Issue:** None reported yet, review for correctness.
*   **PID:** `01 11` (Verified Correct)
*   **Standard Formula:** `A * 100 / 255`, where A is the first data byte = Throttle %.
*   **Current Implementation (`OBDService.kt::parseThrottlePositionResponse`):**
    ```kotlin
    private fun parseThrottlePositionResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) { // Potential issue: Should check for >= 3
            return (values[1].toIntOrNull(16) ?: 0) * 100 / 255 // Potential issue: Indexing. Formula correct.
        }
        throw Exception("Invalid throttle position response")
    }
    ```
*   **Investigation:**
    *   [Date TBD] Formula `A * 100 / 255` is correct.
    *   [Date TBD] Parsing logic indexing needs verification. DriveWise example uses `substring(4, 6)`.
*   **Resolution:**
    *   [Date TBD]
*   **Status:** Pending Investigation

---

## 8. Fuel Pressure (PID `010A`)

*   **Reported Issue:** None reported yet, review for correctness.
*   **PID:** `01 0A` (Verified Correct)
*   **Standard Formula:** `A * 3`, where A is the first data byte = Pressure in kPa.
*   **Current Implementation (`OBDService.kt::parseFuelPressureResponse`):**
    ```kotlin
    private fun parseFuelPressureResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) { // Potential issue: Should check for >= 3
            return values[1].toIntOrNull(16) ?: 0 // Potential issue: Indexing. Formula is incorrect (should be * 3)
        }
        throw Exception("Invalid fuel pressure response")
    }
    ```
*   **Investigation:**
    *   [Date TBD] Formula is incorrect in the code. Should be `A * 3`.
    *   [Date TBD] Parsing logic indexing needs verification.
*   **Resolution:**
    *   [Date TBD] Correct formula. Adjust parsing index if necessary.
*   **Status:** Pending Investigation

---

## 9. Barometric Pressure (PID `0133`)

*   **Reported Issue:** None reported yet, review for correctness.
*   **PID:** `01 33` (Verified Correct)
*   **Standard Formula:** `A`, where A is the first data byte = Pressure in kPa.
*   **Current Implementation (`OBDService.kt::parseBaroPressureResponse`):**
    ```kotlin
    private fun parseBaroPressureResponse(response: String): Int {
        val values = response.split(" ")
        if (values.size >= 2) { // Potential issue: Should check for >= 3
            return values[1].toIntOrNull(16) ?: 0 // Potential issue: Indexing. Formula correct.
        }
        throw Exception("Invalid barometric pressure response")
    }
    ```
*   **Investigation:**
    *   [Date TBD] Formula `A` is correct.
    *   [Date TBD] Parsing logic indexing needs verification.
*   **Resolution:**
    *   [Date TBD]
*   **Status:** Pending Investigation

---

## 10. Battery Voltage (PID `0142`)

*   **Reported Issue:** None reported yet, review for correctness. Note: This PID might not be universally supported. DriveWise used `015B`.
*   **PID:** `01 42` (Used by CarDash) vs `01 5B` (Used by DriveWise). `0142` is Control Module Voltage. `015B` is Hybrid Battery Pack Remaining Life (not voltage). Let's stick with `0142` for now but be aware it might not work on all cars.
*   **Standard Formula (for 0142):** `((A * 256) + B) / 1000`, where A and B are the first two data bytes = Voltage.
*   **Current Implementation (`OBDService.kt::parseBatteryVoltageResponse`):**
    ```kotlin
    private fun parseBatteryVoltageResponse(response: String): Float {
        val values = response.split(" ")
        if (values.size >= 2) { // Potential issue: Should check for >= 3
            // Potential issue: Indexing. Formula is incorrect (should be ((A*256)+B)/1000). Current code only uses one byte and divides by 10.
            return values[1].toIntOrNull(16)?.let { it / 10f } ?: 0f
        }
        throw Exception("Invalid battery voltage response")
    }
    ```
*   **Investigation:**
    *   [Date TBD] Formula is incorrect in the code. Should be `((A * 256) + B) / 1000.0f`.
    *   [Date TBD] Parsing logic indexing needs verification and needs to read two bytes (A and B).
*   **Resolution:**
    *   [Date TBD] Correct formula. Adjust parsing to use two bytes and correct indices.
*   **Status:** Pending Investigation
