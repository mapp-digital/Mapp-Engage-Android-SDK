package com.appoxee.internal.ui.push.model

import com.google.common.truth.Truth
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class CategoryTypeTest {

    @Test
    fun `fromString should return valid CategoryType for valid categoryName`() = runTest {
        CategoryType.entries.forEach {
            Truth.assertThat(CategoryType.fromString(it.categoryName)).isEqualTo(it)
        }
    }

    @Test
    fun `fromString should return null for invalid categoryName`()= runTest {
        Truth.assertThat(CategoryType.fromString("invalid_category")).isNull()
    }
}