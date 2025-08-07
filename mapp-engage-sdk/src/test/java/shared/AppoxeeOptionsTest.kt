package shared

import com.appoxee.shared.AppoxeeOptions
import com.google.common.truth.Truth
import io.mockk.spyk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

class AppoxeeOptionsTest {

    private lateinit var sut: AppoxeeOptions

    @Before
    fun setup() {
        sut = spyk(
            AppoxeeOptions(
                server = AppoxeeOptions.Server.TEST_55,
                sdkKey = "12345.sdk",
                appId = "12345",
                tenantId = "0000"
            )
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test compare returns true`() {
        val options = spyk(
            AppoxeeOptions(
                server = AppoxeeOptions.Server.TEST_55,
                sdkKey = "12345.sdk",
                appId = "12345",
                tenantId = "0000"
            )
        )

        val result = sut.areEquals(options)

        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `test compare returns false`() {
        val options = spyk(
            AppoxeeOptions(
                server = AppoxeeOptions.Server.L3,
                sdkKey = "12345.sdk",
                appId = "12345",
                tenantId = "0000"
            )
        )

        val result = sut.areEquals(options)

        Truth.assertThat(result).isFalse()
    }
}