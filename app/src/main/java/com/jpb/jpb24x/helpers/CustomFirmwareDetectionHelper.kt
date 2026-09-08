package com.jpb.jpb24x.helpers

import android.annotation.SuppressLint
import android.os.Build
import java.io.File

data class FirmwareMetadata(val title: String, val details: String)

object CustomFirmwareDetectionHelper {

    /**
     * Tries multiple access strategies sequentially to find custom ROM signatures.
     */
    fun detectAdvancedFirmware(): FirmwareMetadata? {
        // Strategy 1: Attempt standard reflection hooks first (Will work on open/public props)
        val initialCheck = findViaSystemPropertiesReflection()
        if (initialCheck != null) return initialCheck

        // Strategy 2: Directly parse physical build configurations to bypass SELinux getprop locks
        val fileCheck = findViaBuildPropFiles()
        if (fileCheck != null) return fileCheck

        // Strategy 3: Fall back to public framework parameters that SELinux never blocks
        return findViaPublicBuildConstants()
    }

    private fun findViaSystemPropertiesReflection(): FirmwareMetadata? {
        val manufacturer = Build.MANUFACTURER

        val customRomMap = getCustomRomPropertyMap()
        for ((propertyKey, romName) in customRomMap) {
            val buildVersion = getSystemPropertyReflection(propertyKey)

            // If a system property key from the map exists on the device, return it immediately
            if (buildVersion.isNotEmpty()) {
                return FirmwareMetadata(title = romName, details = buildVersion)
            }
        }

        if (manufacturer.equals("HUAWEI", ignoreCase = true)) {
            val buildVersion =
                getSystemPropertyReflection("ro.huawei.build.version.incremental")
            return if (buildVersion.isNotEmpty()) FirmwareMetadata(title = "EMUI", details = buildVersion) else null
        }

        if (manufacturer.equals("Xiaomi", ignoreCase = true)) {
            val hyperOsCheck =
                getSystemPropertyReflection("ro.mi.os.version.incremental")
            return if (hyperOsCheck.contains("OS", ignoreCase = true)) {
                FirmwareMetadata(title = "HyperOS", details = hyperOsCheck)
            } else {
                val miuiCheck =
                    getSystemPropertyReflection("ro.build.version.incremental")
                if (miuiCheck.contains("XM", ignoreCase = true)) {
                    FirmwareMetadata(title = "MIUI", details = miuiCheck)
                } else {
                    null
                }
            }
        }

        if (manufacturer.equals("Amazon", ignoreCase = true)) {
            val fireOsCheck = getSystemPropertyReflection("ro.build.mktg.fireos")
                .replace("Fire OS ", "")
            return if (fireOsCheck.isNotEmpty()) FirmwareMetadata(title = "FireOS", details = fireOsCheck) else null
        }

        val caesiumCheck = getSystemPropertyReflection("ro.caesium.version")
        if (caesiumCheck.isNotEmpty()) {
            return FirmwareMetadata(title = "CaesiumOS", details = caesiumCheck)
        }

        if (manufacturer.equals("Samsung", ignoreCase = true)) {
            val oneUi = getSystemPropertyReflection("ro.build.version.oneui")
            if (oneUi.isNotEmpty() && oneUi.length >= 3) {
                return FirmwareMetadata("One UI", "${oneUi[0]}.${oneUi.substring(1, 3).trimStart('0')}")
            }
        }
        if (manufacturer.equals("OnePlus", ignoreCase = true)) {
            val oxygen = getSystemPropertyReflection("ro.rom.version").ifEmpty { getSystemPropertyReflection("ro.oxygen.version") }
            if (oxygen.isNotEmpty()) return FirmwareMetadata("OxygenOS", oxygen)
        }
        return null
    }

    private fun findViaBuildPropFiles(): FirmwareMetadata? {
        // Standard world-readable paths across standard partitions
        val paths = listOf("/system/build.prop", "/product/build.prop", "/vendor/build.prop")
        val romMap = getCustomRomPropertyMap()

        for (path in paths) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    file.useLines { lines ->
                        lines.forEach { line ->
                            if (!line.startsWith("#") && line.contains("=")) {
                                val parts = line.split("=", limit = 2)
                                if (parts.size == 2) {
                                    val key = parts[0].trim()
                                    val value = parts[1].trim()

                                    if (romMap.containsKey(key) && value.isNotEmpty()) {
                                        return FirmwareMetadata("${romMap[key]}", value)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun findViaPublicBuildConstants(): FirmwareMetadata? {
        val displayId = Build.DISPLAY.uppercase()
        val fingerprint = Build.FINGERPRINT.uppercase()

        return when {
            displayId.contains("LINEAGE") || fingerprint.contains("LINEAGE") -> FirmwareMetadata("LineageOS", Build.DISPLAY)
            displayId.contains("CRDROID") || fingerprint.contains("CRDROID") -> FirmwareMetadata("crDroid", Build.DISPLAY)
            displayId.contains("EVOLUTIONX") || displayId.contains("EVOX") -> FirmwareMetadata("Evolution X", Build.DISPLAY)
            displayId.contains("PIXELOS") -> FirmwareMetadata("PixelOS", Build.DISPLAY)
            displayId.contains("ARROW") -> FirmwareMetadata("ArrowOS", Build.DISPLAY)
            displayId.contains("RISING") -> FirmwareMetadata("RisingOS", Build.DISPLAY)
            else -> null
        }
    }

    private fun getCustomRomPropertyMap() = mapOf(
        "ro.lineage.version" to "LineageOS",
        "ro.crdroid.version" to "crDroid",
        "ro.pe.version" to "Pixel Experience",
        "ro.evolution.version" to "Evolution X",
        "ro.arrow.version" to "ArrowOS",
        "ro.havoc.version" to "Havoc-OS",
        "ro.paranoid.version" to "Paranoid Android",
        "ro.rising.version" to "RisingOS"
    )

    @SuppressLint("PrivateApi")
    private fun getSystemPropertyReflection(key: String): String {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java)
            (getMethod.invoke(null, key) as? String)?.trim() ?: ""
        } catch (_: Exception) { "" }
    }
}
