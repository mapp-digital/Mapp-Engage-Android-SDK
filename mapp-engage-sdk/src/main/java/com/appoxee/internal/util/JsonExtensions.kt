package com.appoxee.internal.util

import org.json.JSONArray
import org.json.JSONObject
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
                    if (!line.isNullOrEmpty()) {
                        sb.append(line)
                    }
                } while (line != null)
            } catch (ignored: Exception) {
            }
        }
    }
    return sb.toString()
}

fun JSONObject.getNullableString(name: String): String? {
    if (!this.has(name)) return null

    if ("null".equals(this.getString(name), true)) return null

    return this.getString(name)
}

fun JSONObject.getStringOrEmpty(name: String): String {
    return this.getNullableString(name) ?: ""
}

fun JSONObject.getLongOrDefault(name: String, default: Long = 0L): Long {
    if (!this.has(name)) return default
    return this.getLong(name)
}

fun <Value> JSONObject.toMap(): Map<String, Value> {
    val map = mutableMapOf<String, Value>()
    this.keys().forEach {
        map.put(it, this[it] as Value)
    }
    return map
}

inline fun <T> JSONArray.toList(parser: (JSONObject) -> T): List<T> {
    val list = mutableListOf<T>()
    for (i in 0 until this.length()) {
        val item = parser(this.getJSONObject(i))
        list.add(item)
    }
    return list
}