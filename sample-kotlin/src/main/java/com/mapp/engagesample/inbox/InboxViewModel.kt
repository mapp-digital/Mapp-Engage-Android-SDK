package com.mapp.engagesample.inbox

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appoxee.Appoxee
import com.appoxee.internal.model.response.inbox.InboxMessage
import com.appoxee.internal.model.response.inbox.MessageStatus
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.internal.toImmutableList

class InboxViewModel : ViewModel() {
    private val tag = InboxViewModel::class.java.simpleName

    private val messages = mutableListOf<InboxMessage>()
    private val state = MutableStateFlow(InboxStateUI())
    val stateUi: StateFlow<InboxStateUI> = state
    private val coroutineContext =
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
            Log.e(tag, "EXCEPTION: $throwable")
            state.value = InboxStateUI(error = throwable.message)
        }

    init {
        viewModelScope.launch(coroutineContext) {
            state.value = InboxStateUI(isLoading = true)
            messages.clear()
            val result = Appoxee.instance().fetchInboxMessages().asSuspend()
            if (result.isSuccess()) {
                result.getData()?.messages?.let {
                    messages.addAll(it)
                }
                state.value = InboxStateUI(messages = messages.toImmutableList())
            } else {
                val error = result.getError()?.message
                state.value = InboxStateUI(error = error)
            }
        }
    }

    fun updateMessageStatus(message: InboxMessage, index:Int, messageStatus: MessageStatus) {
        state.value = InboxStateUI(isLoading = true)
        viewModelScope.launch(coroutineContext) {
            val result =
                Appoxee.instance().updateInboxMessageStatus(message, messageStatus).asSuspend()
            if (result.isSuccess()) {
                messages[index] = message.setStatus(messageStatus)
                state.value = InboxStateUI(messages = messages.toImmutableList())
            } else {
                val error = result.getError()?.message
                state.value = InboxStateUI(error = error)
            }
        }
    }
}