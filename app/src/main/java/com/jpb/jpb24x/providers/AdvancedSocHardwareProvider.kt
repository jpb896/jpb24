package com.jpb.jpb24x.providers

import android.os.Build
import java.io.BufferedReader
import java.io.FileReader

class AdvancedSocHardwareProvider : SocHardwareProvider {

    override fun getPossibleSocIds(): List<String> {
        val candidates = mutableSetOf<String>()

        // 1. Check primary modern system property fields
        candidates.add(Build.HARDWARE)
        candidates.add(Build.BOARD)

        // 2. Fallback check for older chips or custom ROMs via system properties
        getSystemProperty("ro.board.platform")?.let { candidates.add(it) }
        getSystemProperty("ro.hardware")?.let { candidates.add(it) }

        // 3. Last resort: Parse /proc/cpuinfo for the Hardware string
        getSocFromCpuInfo()?.let { candidates.add(it) }

        // Clean, normalize, and filter empty strings
        return candidates
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() && it != "UNKNOWN" }
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(listOf("/system/bin/getprop", key).toTypedArray())
            process.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) {
            null
        }
    }

    private fun getSocFromCpuInfo(): String? {
        return try {
            BufferedReader(FileReader("/proc/proc/cpuinfo")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("Hardware", ignoreCase = true)) {
                        val parts = line.split(":")
                        if (parts.size > 1) return parts[1].trim()
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}