package com.appoxee.internal.util

import org.json.JSONArray

fun JSONArray?.toList(): List<String> {
    if (this == null || this.length() == 0) return emptyList()
    val data = mutableListOf<String>()
    for (i in 0 until this.length()) {
        data.add(this[i].toString())
    }
    return data
}