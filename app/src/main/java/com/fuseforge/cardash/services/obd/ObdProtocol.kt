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
    const val BATTERY_VOLT = "01 42"
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

    fun parseCoolantTemp(response: String): Int? {
        return try {
            val pattern = "(?:41 05|4105) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(response) ?: return null
            match.groupValues[1].toInt(16) - 40
        } catch (e: Exception) { null }
    }

    fun parseVoltage(response: String): Float? {
        return try {
            if (response.contains("V")) {
                // Handle AT RV response
                val pattern = "([0-9]+\\.?[0-9]*)V?".toRegex(RegexOption.IGNORE_CASE)
                pattern.find(response)?.groupValues?.get(1)?.toFloatOrNull()
            } else {
                // Handle PID 01 42 response
                val pattern = "(?:41 42|4142) ?([0-9A-F]{2}) ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
                val match = pattern.find(response) ?: return null
                val (hexA, hexB) = match.destructured
                ((hexA.toInt(16) * 256) + hexB.toInt(16)) / 1000f
            }
        } catch (e: Exception) { null }
    }

    fun parseGenericPercentage(response: String, expectedHeader: String): Int? {
        return try {
            val pattern = "$expectedHeader ?([0-9A-F]{2})".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(response) ?: return null
            (match.groupValues[1].toInt(16) * 100) / 255
        } catch (e: Exception) { null }
    }
}
