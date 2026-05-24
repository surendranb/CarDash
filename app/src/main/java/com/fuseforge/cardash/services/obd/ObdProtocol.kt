package com.fuseforge.cardash.services.obd

import android.util.Log

/**
 * Pure protocol utility for OBD-II PID parsing.
 * Decoupled from I/O and Socket logic for high-fidelity unit testing.
 */
object ObdProtocol {
    private const val TAG = "ObdProtocol"

    // Core PIDs
    const val RPM = "01 0C"
    const val SPEED = "01 0D"
    const val ENGINE_LOAD = "01 04"
    const val COOLANT_TEMP = "01 05"
    const val FUEL_LEVEL = "01 2F"
    const val THROTTLE_POS = "01 11"
    const val BATTERY_VOLT = "AT RV"
    const val BARO_PRESS = "01 33"
    const val INTAKE_AIR_TEMP = "01 0F"
    const val FUEL_PRESS = "01 0A"

    // AT Commands
    const val RESET = "ATZ"
    const val ECHO_OFF = "ATE0"
    const val PROTOCOL_AUTO = "ATSP0"
    const val NATIVE_VOLTAGE = "AT RV"

    fun parseRpm(response: String): Int? {
        return try {
            val pattern = "(?:41 0C|410C) ?([0-9A-F]{2}) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(response) ?: return null
            val (hexA, hexB) = match.destructured
            ((hexA.toInt(16) * 256) + hexB.toInt(16)) / 4
        } catch (e: Exception) { null }
    }

    fun parseSpeed(response: String): Int? {
        return try {
            val pattern = "(?:41 0D|410D) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(response) ?: return null
            match.groupValues[1].toInt(16)
        } catch (e: Exception) { null }
    }

    fun parseEngineLoad(response: String): Int? {
        return try {
            val pattern = "(?:41 04|4104) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(response) ?: return null
            (match.groupValues[1].toInt(16) * 100) / 255
        } catch (e: Exception) { null }
    }

    fun parseTemp(response: String, expectedHeader: String): Int? {
        return try {
            val compactHeader = expectedHeader.replace(" ", "")
            val pattern = "(?:$expectedHeader|$compactHeader) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(response) ?: return null
            match.groupValues[1].toInt(16) - 40
        } catch (e: Exception) { null }
    }

    fun parseCoolantTemp(response: String): Int? {
        return parseTemp(response, "41 05")
    }

    fun parseIntakeAirTemp(response: String): Int? {
        return parseTemp(response, "41 0F")
    }

    fun parseVoltage(response: String): Float? {
        return try {
            val decimalPattern = "^\\s*([0-9]+\\.?[0-9]*)\\s*V?\\s*$".toRegex(RegexOption.IGNORE_CASE)
            val match = decimalPattern.find(response)
            if (match != null) {
                match.groupValues[1].toFloatOrNull()
            } else {
                // Handle PID 01 42 response
                val pattern = "(?:41 42|4142) ?([0-9A-F]{2}) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
                val hexMatch = pattern.find(response) ?: return null
                val (hexA, hexB) = hexMatch.destructured
                ((hexA.toInt(16) * 256) + hexB.toInt(16)) / 1000f
            }
        } catch (e: Exception) { null }
    }

    fun parseGenericPercentage(response: String, expectedHeader: String): Int? {
        return try {
            val compactHeader = expectedHeader.replace(" ", "")
            val pattern = "(?:$expectedHeader|$compactHeader) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(response) ?: return null
            (match.groupValues[1].toInt(16) * 100) / 255
        } catch (e: Exception) { null }
    }

    fun parseBaroPressure(response: String): Int? {
        return try {
            val pattern = "(?:41 33|4133) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(response) ?: return null
            match.groupValues[1].toInt(16) // Direct kPa mapping (A)
        } catch (e: Exception) { null }
    }

    fun parseFuelPressure(response: String): Int? {
        return try {
            val pattern = "(?:41 0A|410A) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(response) ?: return null
            match.groupValues[1].toInt(16) * 3 // Formula: A * 3 kPa
        } catch (e: Exception) { null }
    }

    fun parseTroubleCodes(response: String): List<String> {
        val dtcs = mutableListOf<String>()
        val lines = response.split("\r", "\n")
        for (line in lines) {
            val clean = line.replace(" ", "").trim()
            if (clean.uppercase().startsWith("43")) {
                // Strip the "43" header
                val data = clean.substring(2)
                var i = 0
                while (i + 3 < data.length) {
                    val hexCode = data.substring(i, i + 4).uppercase()
                    if (hexCode != "0000") {
                        val firstChar = hexCode[0]
                        val prefix = when (firstChar) {
                            '0' -> "P0"
                            '1' -> "P1"
                            '2' -> "P2"
                            '3' -> "P3"
                            '4' -> "C0"
                            '5' -> "C1"
                            '6' -> "C2"
                            '7' -> "C3"
                            '8' -> "B0"
                            '9' -> "B1"
                            'A' -> "B2"
                            'B' -> "B3"
                            'C' -> "U0"
                            'D' -> "U1"
                            'E' -> "U2"
                            'F' -> "U3"
                            else -> null
                        }
                        if (prefix != null) {
                            dtcs.add(prefix + hexCode.substring(1))
                        }
                    }
                    i += 4
                }
            }
        }
        return dtcs
    }
}
