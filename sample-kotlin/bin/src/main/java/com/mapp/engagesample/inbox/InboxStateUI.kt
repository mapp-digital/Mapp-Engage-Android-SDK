package com.mapp.engagesample.inbox

import com.appoxee.shared.InboxMessage

data class InboxStateUI(
    val messages: List<InboxMessage>? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

