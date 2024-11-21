package com.mapp.engagesample.inbox

import com.appoxee.internal.model.response.inbox.InboxMessage

data class InboxStateUI(
    val messages: List<InboxMessage>? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)