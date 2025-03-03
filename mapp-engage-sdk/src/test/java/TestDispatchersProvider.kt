import com.appoxee.internal.util.DispatchersProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestDispatcher

class TestDispatchersProvider(private val dispatcher: TestDispatcher) :
    DispatchersProvider {
    override val ioDispatcher: CoroutineDispatcher
        get() = dispatcher
    override val mainDispatcher: CoroutineDispatcher
        get() = dispatcher
    override val defaultDispatcher: CoroutineDispatcher
        get() = dispatcher
}