import com.appoxee.internal.util.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class TestDispatchers @OptIn(ExperimentalCoroutinesApi::class) constructor(
    override val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    override val mainDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    override val defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
) : Dispatchers {
}