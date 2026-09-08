package com.jpb.jpb24x.helpers

import android.annotation.SuppressLint
import android.os.Build
import java.io.File

data class FirmwareMetadata(val title: String, val details: String, val fullVer: String? = null, val codename: String? = null, val region: String? = null, val releaseType: String? = null, val sehi: String? = null, val sem: String? = null, val sep: String? = null)

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

        val caesiumCheck = getSystemPropertyReflection("ro.caesium.build.version")
        if (caesiumCheck.isNotEmpty()) {
            return FirmwareMetadata(title = "CaesiumOS", details = caesiumCheck, codename = getSystemPropertyReflection("ro.caesium.codename"), fullVer = getSystemPropertyReflection("ro.caesium.version"), releaseType = getSystemPropertyReflection("ro.caesium.track"))
        }

        // 1. Iterate through Custom ROM Configs
        for ((romName, versionKey, codenameKeys, fullBuildKeys) in getCustomRomList()) {
            val version = getSystemPropertyReflection(versionKey)

            if (version.isNotEmpty()) {
                // Find specific ROM codename or fall back to device codename
                val romCodename = codenameKeys
                    .map { getSystemPropertyReflection(it) }
                    .firstOrNull { it.isNotEmpty() }

                // Find specific ROM build ID or fall back to system build display ID
                val fullBuildId = fullBuildKeys
                    .map { getSystemPropertyReflection(it) }
                    .firstOrNull { it.isNotEmpty() }
                if (getSystemPropertyReflection("ro.miui.build.region") == "") {
                    return FirmwareMetadata(
                        title = romName,
                        details = version,
                        codename = romCodename,
                        fullVer = fullBuildId,
                    )
                } else {
                        return FirmwareMetadata(
                            title = romName,
                            details = version,
                            codename = romCodename,
                            fullVer = fullBuildId,
                            region = getSystemPropertyReflection("ro.miui.build.region")
                        )

                }
            }
        }

        if (manufacturer.equals("HUAWEI", ignoreCase = true)) {
            val buildVersion =
                getSystemPropertyReflection("ro.huawei.build.version.incremental")
            return if (buildVersion.isNotEmpty()) FirmwareMetadata(title = "EMUI", details = buildVersion) else null
        }

        if (manufacturer.equals("Xiaomi", ignoreCase = true)) {
            val hyperOsCheck =
                getSystemPropertyReflection("ro.mi.os.version.code")
            return if (hyperOsCheck.contains("OS", ignoreCase = true)) {
                FirmwareMetadata(title = "HyperOS", details = hyperOsCheck, fullVer = getSystemPropertyReflection("ro.mi.os.version.incremental"), region = getSystemPropertyReflection("ro.miui.build.region"))
            } else {
                val miuiCheck =
                    getSystemPropertyReflection("ro.build.version.incremental")
                if (miuiCheck.contains("XM", ignoreCase = true)) {
                    FirmwareMetadata(title = "MIUI", details = miuiCheck, region = getSystemPropertyReflection("ro.miui.build.region"))
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

        if (manufacturer.equals("Samsung", ignoreCase = true)) {
            val oneUi = getSystemPropertyReflection("ro.build.version.oneui")
            if (oneUi.isNotEmpty() && oneUi.length >= 3) {
                return FirmwareMetadata("One UI", "${oneUi[0]}.${oneUi.substring(1, 3).trimStart('0')}", sehi = getSystemPropertyReflection("ro.system.build.version.sehi"), sem = getSystemPropertyReflection("ro.build.version.sem"), sep = getSystemPropertyReflection("ro.build.version.sep"))
            }
        }
        if (manufacturer.equals("OnePlus", ignoreCase = true)) {
            val oxygen = getSystemPropertyReflection("ro.rom.version").ifEmpty { getSystemPropertyReflection("ro.oxygen.version") }
            if (oxygen.isNotEmpty()) return FirmwareMetadata("OxygenOS", oxygen)
        }
        return null
    }

    private fun findViaBuildPropFiles(): FirmwareMetadata? {
        // Partition paths where build properties are typically stored
        val paths = listOf(
            "/system/build.prop",
            "/product/build.prop",
            "/vendor/build.prop",
            "/system_ext/build.prop"
        )

        // 1. Read and combine all build.prop files into a single map
        val buildPropsMap = mutableMapOf<String, String>()

        for (path in paths) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    file.useLines { lines ->
                        lines.forEach { line ->
                            val trimmed = line.trim()
                            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                                val parts = trimmed.split("=", limit = 2)
                                if (parts.size == 2) {
                                    val key = parts[0].trim()
                                    val value = parts[1].trim()
                                    if (key.isNotEmpty() && value.isNotEmpty()) {
                                        buildPropsMap[key] = value
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (buildPropsMap.isEmpty()) return null

        // Fetch hardware fallbacks directly from the parsed file map or Build fallback
        val defaultDeviceCodename = buildPropsMap["ro.product.device"]
            ?: buildPropsMap["ro.product.vendor.device"]
            ?: Build.DEVICE

        val defaultFullBuildId = buildPropsMap["ro.build.display.id"]
            ?: Build.DISPLAY

        // 2. Iterate through Custom ROM list to find matching properties in the file map
        for ((romName, versionKey, codenameKeys, fullBuildKeys) in getCustomRomList()) {
            val version = buildPropsMap[versionKey]

            if (!version.isNullOrEmpty()) {
                // Find specific ROM codename or fall back
                val romCodename = codenameKeys
                    .mapNotNull { buildPropsMap[it] }
                    .firstOrNull { it.isNotEmpty() }
                    ?: defaultDeviceCodename

                // Find specific ROM build ID or fall back
                val fullBuildId = fullBuildKeys
                    .mapNotNull { buildPropsMap[it] }
                    .firstOrNull { it.isNotEmpty() }
                    ?: defaultFullBuildId

                return FirmwareMetadata(
                    title = romName,
                    details = version,
                    codename = romCodename,
                    fullVer = fullBuildId
                )
            }
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

    private fun getCustomRomList() = listOf(
        RomPropertyConfig(
            romName = "RisingOS",
            versionKey = "ro.rising.version",
            codenameKeys = listOf("ro.rising.code"),
            fullBuildKeys = listOf("ro.rising.build.version")
        ),
        RomPropertyConfig(
            romName = "LineageOS",
            versionKey = "ro.lineage.version",
            codenameKeys = listOf("ro.lineage.device"),
            fullBuildKeys = listOf("ro.lineage.display.version")
        ),
        RomPropertyConfig(
            romName = "crDroid",
            versionKey = "ro.crdroid.version",
            codenameKeys = listOf("ro.crdroid.device"),
            fullBuildKeys = listOf("ro.crdroid.display.version")
        ),
        RomPropertyConfig(
            romName = "Pixel Experience",
            versionKey = "ro.pe.version"
        ),
        RomPropertyConfig(
            romName = "Evolution X",
            versionKey = "ro.evolution.version",
            codenameKeys = listOf("ro.evolution.device")
        ),
        RomPropertyConfig(
            romName = "ArrowOS",
            versionKey = "ro.arrow.version"
        ),
        RomPropertyConfig(
            romName = "Havoc-OS",
            versionKey = "ro.havoc.version"
        ),
        RomPropertyConfig(
            romName = "Paranoid Android",
            versionKey = "ro.paranoid.version"
        ),
        RomPropertyConfig(
            romName = "CaesiumOS (Conception)",
            versionKey = "ro.caesium.version",
            codenameKeys = listOf("ro.caesium.codename")
        ),
        RomPropertyConfig(
            romName = "CharaROM",
            versionKey = "ro.chara.version"
        )
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

data class RomPropertyConfig(
    val romName: String,
    val versionKey: String,
    val codenameKeys: List<String> = emptyList(),
    val fullBuildKeys: List<String> = emptyList()
)