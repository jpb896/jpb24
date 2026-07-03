package com.jpb.jpb24x.helpers

import java.io.File
import java.io.FileFilter
import java.util.regex.Pattern

public class SoCHelper {
    companion object {
        var sLastCpuCoreCount: Int = -1
        public fun calcCpuCoreCount(): Int {
            if (sLastCpuCoreCount >= 1) {
                // キャッシュさせる
                return sLastCpuCoreCount
            }

            try {
                // Get directory containing CPU info
                val dir = File("/sys/devices/system/cpu/")
                // Filter to only list the devices we care about
                val files = dir.listFiles(object : FileFilter {
                    override fun accept(pathname: File): Boolean {
                        //Check if filename is "cpu", followed by a single digit number
                        if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                            return true
                        }
                        return false
                    }
                })

                // Return the number of cores (virtual CPU devices)
                sLastCpuCoreCount = files!!.size
            } catch (e: Exception) {
                sLastCpuCoreCount = Runtime.getRuntime().availableProcessors()
            }

            return sLastCpuCoreCount
        }
    }
}