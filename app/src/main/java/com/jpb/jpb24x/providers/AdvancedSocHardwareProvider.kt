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
            .map { cleanAndNormalizeSocId(it) }
            .filter { it.isNotEmpty() && it != "UNKNOWN" }
            .distinct()
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

    private fun cleanAndNormalizeSocId(rawId: String): String {
        var clean = rawId.trim().uppercase()

        // Translate known low-level Qualcomm codenames to commercial SoC IDs
        when (clean) {
            "BLAIR", "HOLI" -> return "SM6375"   // Snapdragon 695 5G / 4 Gen 1 platform
            "BENGAL"        -> return "SM6115"   // Snapdragon 662 / 460
            "KHADGE"        -> return "SM4350"   // Snapdragon 480
            "TARO"          -> return "SM8450"   // Snapdragon 8 Gen 1
            "CAPE"          -> return "SM8475"   // Snapdragon 8+ Gen 1
            "KALAMA"        -> return "SM8550"   // Snapdragon 8 Gen 2
            "PINEAPPLE"     -> return "SM8650"   // Snapdragon 8 Gen 3
        }

        if (clean.contains("MEDIATEK") || clean.contains("MTK")) {
            clean = clean.replace("MEDIATEK", "").replace("MTK", "").replace("(", "").replace(")", "").trim()
        }
        return clean
    }
}