package com.appoxee.internal.model.response.inbox

import com.appoxee.internal.model.request.events.TrackingKey

enum class MessageStatus(val status: String) {
    READ("READ"), UNREAD("UNREAD"), DELETED("DELETED");

    fun getName(): String {
        return status
    }

    override fun toString(): String {
        return "MessageStatus(status='$status')"
    }

    internal fun toTrackingKey(): TrackingKey {
        return when (status.lowercase()) {
            "read" -> TrackingKey.INBOX_MESSAGE_READ
            "deleted" -> TrackingKey.INBOX_MESSAGE_DELETED
            else -> TrackingKey.INBOX_MESSAGE_UNREAD
        }
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

