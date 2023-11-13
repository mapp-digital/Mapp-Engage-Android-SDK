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
    return try {
        this.getLong(name)
    } catch (e: Exception) {
        default
    }
}

fun JSONObject.getNullableLong(name: String): Long? {
    if (!this.has(name)) return null
    return try {
        this.getLong(name)
    } catch (e: Exception) {
        null
    }
}

fun <Value> JSONObject.toMap(excludeNulls: Boolean = false): Map<String, Value?> {
    val map = mutableMapOf<String, Value>()
    this.keys().forEach {
        val value = this[it] as Value?
        if (!excludeNulls || value != null) {
            map[it] = value!!
        }
    }
    return map
}

inline fun <T> JSONObject.arrayToList(name: String, parser: (JSONObject) -> T): List<T> {
    val array = if (this.has(name)) this.optJSONArray(name) else JSONArray()
    val list = mutableListOf<T>()
    for (i in 0 until array.length()) {
        list.add(parser.invoke(array.getJSONObject(i)))
    }
    return list
}

inline fun <reified T> JSONObject.arrayToMap(
    name: String,
    parser: (JSONObject) -> T
): Map<String, T> {
    val jsonArr = this.optJSONArray(name) ?: JSONArray()
    val map = mutableMapOf<String, T>()
    for (i in 0 until jsonArr.length()) {
        val item = jsonArr.getJSONObject(i)
        item.keys().forEach {
            if (T::class == String::class) {
                map[it] = item.getString(it) as T
            } else {
                val value = parser.invoke(item.getJSONObject(it))
                map[it] = value
            }
        }
    }
    return map
}

fun String?.parseAsJSON(): JSONObject {
    if (this.isNullOrEmpty()) return JSONObject()
    return try {
        JSONObject(this)
    } catch (e: Exception) {
        JSONObject().apply {
            put("data", this)
        }
    }
}