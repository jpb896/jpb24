package com.jpb.jpb24x.helpers

import java.io.File
import java.util.regex.Pattern

class SoCHelper {
    companion object {
        var sLastCpuCoreCount: Int = -1
        fun calcCpuCoreCount(): Int {
            if (sLastCpuCoreCount >= 1) {
                // キャッシュさせる
                return sLastCpuCoreCount
            }

            try {
                // Get directory containing CPU info
                val dir = File("/sys/devices/system/cpu/")
                // Filter to only list the devices we care about
                val files = dir.listFiles { pathname -> //Check if filename is "cpu", followed by a single digit number
                    Pattern.matches("cpu[0-9]", pathname.getName())
                }

                // Return the number of cores (virtual CPU devices)
                sLastCpuCoreCount = files!!.size
            } catch (_: Exception) {
                sLastCpuCoreCount = Runtime.getRuntime().availableProcessors()
            }

            return sLastCpuCoreCount
        }
    }
}