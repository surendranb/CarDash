package com.fuseforge.cardash

import com.fuseforge.cardash.services.obd.ObdProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObdProtocolTest {

    @Test
    fun testParseCoolantTemp() {
        // 41 05 3C -> 0x3C = 60 -> 60 - 40 = 20
        assertEquals(20, ObdProtocol.parseCoolantTemp("41 05 3C"))
        assertEquals(20, ObdProtocol.parseCoolantTemp("41053C"))
        assertEquals(20, ObdProtocol.parseCoolantTemp("  41053C  "))
        assertNull(ObdProtocol.parseCoolantTemp("41 0F 3C")) // Intake Air Temp PID
        assertNull(ObdProtocol.parseCoolantTemp("invalid"))
    }

    @Test
    fun testParseIntakeAirTemp() {
        // 41 0F 3C -> 0x3C = 60 -> 60 - 40 = 20
        assertEquals(20, ObdProtocol.parseIntakeAirTemp("41 0F 3C"))
        assertEquals(20, ObdProtocol.parseIntakeAirTemp("410F3C"))
        assertNull(ObdProtocol.parseIntakeAirTemp("41 05 3C")) // Coolant Temp PID
        assertNull(ObdProtocol.parseIntakeAirTemp("invalid"))
    }

    @Test
    fun testParseVoltage() {
        // AT RV response
        assertEquals(13.8f, ObdProtocol.parseVoltage("13.8V") ?: 0f, 0.01f)
        assertEquals(12.4f, ObdProtocol.parseVoltage("12.4") ?: 0f, 0.01f)
        
        // PID 01 42 response: 41 42 0D A6 -> 0x0DA6 = 3494 -> 3.494V (Note: 01 42 is control module voltage, usually around 12-14V but depends on representation)
        // 0x30E4 = 12516 -> 12.516V
        assertEquals(12.516f, ObdProtocol.parseVoltage("41 42 30 E4") ?: 0f, 0.01f)
        assertEquals(12.516f, ObdProtocol.parseVoltage("414230E4") ?: 0f, 0.01f)
    }

    @Test
    fun testParseGenericPercentage() {
        // Expected header "41 2F" (Fuel level)
        // 41 2F 7F -> 0x7F = 127 -> (127 * 100) / 255 = 49%
        assertEquals(49, ObdProtocol.parseGenericPercentage("41 2F 7F", "41 2F"))
        assertEquals(49, ObdProtocol.parseGenericPercentage("412F7F", "41 2F"))
        
        // Expected header "41 11" (Throttle position)
        assertEquals(100, ObdProtocol.parseGenericPercentage("41 11 FF", "41 11"))
        assertEquals(0, ObdProtocol.parseGenericPercentage("411100", "41 11"))
    }

    @Test
    fun testParseTroubleCodes() {
        // Single code P0133 encoded as 01 33
        val response1 = "43 01 33 00 00 00 00"
        val dtcs1 = ObdProtocol.parseTroubleCodes(response1)
        assertEquals(listOf("P0133"), dtcs1)

        // Multiple codes: P0133 (0133), C0101 (4101), U0100 (C100)
        val response2 = "43 01 33 41 01 C1 00"
        val dtcs2 = ObdProtocol.parseTroubleCodes(response2)
        assertEquals(listOf("P0133", "C0101", "U0100"), dtcs2)

        // Multiple lines response (e.g. multiline from ELM)
        val response3 = "43 01 33 00 00\r43 03 00 00 00"
        val dtcs3 = ObdProtocol.parseTroubleCodes(response3)
        assertEquals(listOf("P0133", "P0300"), dtcs3)

        // No codes
        val response4 = "43 00 00 00 00 00 00"
        val dtcs4 = ObdProtocol.parseTroubleCodes(response4)
        assertEquals(emptyList<String>(), dtcs4)
    }
}
