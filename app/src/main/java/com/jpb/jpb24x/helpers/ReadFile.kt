package com.jpb.jpb24x.helpers

import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader


class ReadFile {
    private fun readIntegerFile(filePath: String): Int {
        try {
            val reader = BufferedReader(
                InputStreamReader(FileInputStream(filePath)), 1000
            )
            val line = reader.readLine()
            reader.close()

            return line.toInt()
        } catch (e: Exception) {
            return 0
        }
    }
}