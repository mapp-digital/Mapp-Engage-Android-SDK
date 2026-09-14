package com.appoxee.internal.ui.push.model

import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.Category
import com.appoxee.internal.storage.Storage
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CategoriesFactoryTest {

    private val storage = mockk<Storage>()

    @Test
    fun get_categories_returns_stored_configuration() = runTest {
        val configured = listOf(
            Category(
                categoryId = 42,
                categoryType = CategoryType.APX_CUSTOM_PUSH_1,
                title = "Custom",
                type = 1,
            )
        )
        val appConfig = mockk<AppConfigPayload> {
            every { categories } returns configured
        }
        coEvery { storage.getAppConfig() } returns appConfig

        assertThat(CategoriesFactory(storage).getCategories()).isEqualTo(configured)
    }

    @Test
    fun get_categories_returns_defaults_without_stored_configuration() = runTest {
        coEvery { storage.getAppConfig() } returns null

        val categories = CategoriesFactory(storage).getCategories()

        assertThat(categories).isNotEmpty()
        assertThat(categories.map { it.categoryType }).contains(CategoryType.APX_YES_NO_OPEN)
    }
}
