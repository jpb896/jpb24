package com.jpb.jpb24x.helpers

import android.content.Context
import com.jpb.jpb24x.providers.SocHardwareProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SocRepository(
    private val context: Context,
    private val hardwareProvider: SocHardwareProvider
) {
    private val jsonConfig = Json { ignoreUnknownKeys = true }
    private var socMap: Map<String, SocInfo>? = null

    private suspend fun loadSocData(): Map<String, SocInfo> = withContext(Dispatchers.IO) {
        socMap?.let { return@withContext it }
        try {
            val jsonString = context.assets.open("soc_process_map.json").bufferedReader().use { it.readText() }
            val socList = jsonConfig.decodeFromString<List<SocInfo>>(jsonString)
            val mappedData = socList.associateBy { it.socId.uppercase() }
            socMap = mappedData
            mappedData
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** Resolves the system hardware against the mapped JSON library file */
    suspend fun resolveDeviceSoc(): SocInfo? {
        val dataMap = loadSocData()
        val deviceCandidates = hardwareProvider.getPossibleSocIds()

        // Return the first candidate that successfully hits an entry in your JSON map
        return deviceCandidates.firstNotNullOfOrNull { candidate -> dataMap[candidate] }
    }
}