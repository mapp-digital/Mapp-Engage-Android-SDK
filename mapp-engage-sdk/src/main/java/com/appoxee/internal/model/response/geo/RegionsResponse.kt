package com.appoxee.internal.model.response.geo

import com.appoxee.internal.util.arrayToList
import com.appoxee.internal.util.getLongOrDefault
import org.json.JSONObject

data class RegionsResponse(val version: Long, val regions: List<Region>) {
    companion object {
        fun fromJSON(jsonObj: JSONObject): RegionsResponse {
            return jsonObj.optJSONObject("get_regions")?.let { json ->
                RegionsResponse(
                    version = json.getLongOrDefault("version"),
                    regions = json.arrayToList(name = "regions", parser = {
                        Region.fromJSON(it)
                    })
                )
            } ?: RegionsResponse(0, emptyList())
        }
    }
}