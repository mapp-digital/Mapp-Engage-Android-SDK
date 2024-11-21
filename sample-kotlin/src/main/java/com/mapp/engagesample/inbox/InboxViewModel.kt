package com.mapp.engagesample.inbox

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appoxee.Appoxee
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InboxViewModel : ViewModel() {
    private val tag = InboxViewModel::class.java.simpleName

    private val _messages = MutableStateFlow(InboxStateUI())
    val messages: StateFlow<InboxStateUI> = _messages
    private val coroutineContext =
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
            Log.e(tag, "EXCEPTION: $throwable")
            _messages.value = InboxStateUI(error = throwable.message)
        }

    init {
        viewModelScope.launch(coroutineContext) {
            _messages.value = InboxStateUI(isLoading = true)
            val result = Appoxee.instance().fetchInboxMessages().asSuspend()
            if (result.isSuccess()) {
                val messages = result.getData()?.messages
                _messages.value = InboxStateUI(messages = messages)
            } else {
                val error = result.getError()?.message
                _messages.value = InboxStateUI(error = error)
            }
        }
    }
}