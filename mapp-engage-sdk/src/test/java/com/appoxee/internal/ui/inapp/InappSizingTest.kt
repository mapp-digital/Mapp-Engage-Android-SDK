package com.appoxee.internal.ui.inapp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InappSizingTest {

    @Test
    fun size_uses_positive_value_and_defaults_null_zero_or_negative_values() {
        assertThat(75.inappSizePercentOrDefault()).isEqualTo(75)
        assertThat(null.inappSizePercentOrDefault()).isEqualTo(DEFAULT_INAPP_SIZE_PERCENT)
        assertThat(0.inappSizePercentOrDefault()).isEqualTo(DEFAULT_INAPP_SIZE_PERCENT)
        assertThat((-1).inappSizePercentOrDefault()).isEqualTo(DEFAULT_INAPP_SIZE_PERCENT)
    }
}
