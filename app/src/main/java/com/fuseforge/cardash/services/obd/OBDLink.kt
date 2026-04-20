package com.fuseforge.cardash.services.obd

import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

/**
 * High-reliability, stateless physical IO link for ELM327 Bluetooth adapters.
 * Optimized for minimal latency and hard-timeout recovery.
 */
class OBDLink(private val socket: BluetoothSocket) {
    private val TAG = "OBDLink"
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    init {
        try {
            inputStream = socket.inputStream
            outputStream = socket.outputStream
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize streams: ${e.message}")
        }
    }

    /**
     * Executes a single command on the car's bus.
     * Blocks the calling coroutine until response or [timeoutMs].
     */
    suspend fun query(command: String, timeoutMs: Long = 2000): String = withContext(Dispatchers.IO) {
        val out = outputStream ?: throw Exception("Output stream unavailable")
        val input = inputStream ?: throw Exception("Input stream unavailable")

        // 1. Drain the pipe to ensure no stale data from previous failed cycles
        while (input.available() > 0) {
            input.read(ByteArray(input.available()))
        }

        // 2. Transmit
        out.write("$command\r".toByteArray())
        out.flush()

        // 3. Collect response with Hard Timeout Watchdog
        val response = StringBuilder()
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val available = input.available()
            if (available > 0) {
                val buffer = ByteArray(available)
                val bytesRead = input.read(buffer)
                if (bytesRead > 0) {
                    val chunk = String(buffer, 0, bytesRead)
                    response.append(chunk)
                    
                    // ELM327 prompt character marks the end of response
                    if (chunk.contains(">")) {
                        break
                    }
                }
            } else {
                delay(10) // Small yield to prevent CPU thrashing
            }
        }

        val result = response.toString().trim()
        if (result.isEmpty()) {
            throw Exception("Timeout or Empty Response for $command")
        }
        
        return@withContext result
    }

    fun isConnected(): Boolean {
        return socket.isConnected
    }

    fun close() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error during link closure: ${e.message}")
        }
    }
}
