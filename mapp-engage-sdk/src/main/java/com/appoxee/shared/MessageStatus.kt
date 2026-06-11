package com.appoxee.shared

enum class MessageStatus(val status: String) {
    READ("READ"), UNREAD("UNREAD"), DELETED("DELETED");

    fun getName(): String {
        return status
    }

    override fun toString(): String {
        return "MessageStatus(status='$status')"
    }
}
