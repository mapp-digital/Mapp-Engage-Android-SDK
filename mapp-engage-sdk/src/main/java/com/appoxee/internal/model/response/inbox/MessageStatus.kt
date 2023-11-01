package com.appoxee.internal.model.response.inbox

enum class MessageStatus(val status: String) {
    READ("READ"), UNREAD("UNREAD"), DELETED("DELETED");

    fun getName(): String {
        return status
    }

    override fun toString(): String {
        return "MessageStatus(status='$status')"
    }

    companion object {
        fun fromName(status: String): MessageStatus {
            return when (status.lowercase()) {
                "read" -> READ
                "deleted" -> DELETED
                else -> UNREAD
            }
        }
    }
}

