package com.appoxee.internal.model.response.inapp

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class LocationTest {

    @Test
    fun fromJSON_defaults_missing_size_to_full_screen_percent() {
        val location = Location.fromJSON(JSONObject("{}"))

        assertThat(location.height).isEqualTo(100)
        assertThat(location.width).isEqualTo(100)
    }

    @Test
    fun fromJSON_defaults_zero_size_to_full_screen_percent() {
        val location = Location.fromJSON(
            JSONObject(
                """
                {
                  "height": 0,
                  "width": 0
                }
                """.trimIndent()
            )
        )

        assertThat(location.height).isEqualTo(100)
        assertThat(location.width).isEqualTo(100)
    }
}
