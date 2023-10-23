package com.appoxee.internal.util

import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

fun JSONArray?.toList(): List<String> {
    if (this == null || this.length() == 0) return emptyList()
    val data = mutableListOf<String>()
    for (i in 0 until this.length()) {
        data.add(this[i].toString())
    }
    return data
}

fun InputStream?.convertToString(): String {
    val sb = StringBuilder()
    this?.let {
        BufferedReader(InputStreamReader(this)).run {
            var line: String?
            try {
                do {
                    line = readLine()
                    sb.append(line)
                } while (line != null)
            } catch (ignored: Exception) {
            }
        }
    }
    return sb.toString()
}